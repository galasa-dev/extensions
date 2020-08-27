/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.model;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;

import dev.galasa.docker.operator.DockerOperatorException;
import dev.galasa.docker.operator.config.EcosystemConfiguration;
import dev.galasa.docker.operator.config.JenkinsConfiguration;

public class JenkinsVolume extends AbstractResource {

    private static final String RESOURCE_NAME = "galasa_jenkins_volume";
    
    private final Api api;

    public JenkinsVolume(Ecosystem ecosystem) {
        super(ecosystem);
        
        this.api = ecosystem.getResource(Api.class);
        this.api.addDependency(this);
    }

    @Override
    public void checkResourceDefined() throws DockerOperatorException {

        DockerClient dockerClient = getEcosystem().getDockerClient();

        try {
            dockerClient.inspectVolumeCmd(RESOURCE_NAME).exec();

            return;
        } catch(NotFoundException e) {
            System.out.println("Jenkins volume is missing");
        } catch(Exception e) {
            throw new DockerOperatorException("Problem inspecting the Jenkins volume resource", e);
        }

        for(AbstractResource resource : getDependencies()) {
            resource.dependencyChanging(DependencyEvent.DEFINING, this);
        }

        try {
            System.out.println("Creating Jenkins volume seeding container");
            EcosystemConfiguration ecoConfig = getEcosystem().getConfiguration();
            JenkinsConfiguration jenkinsConfig = ecoConfig.getJenkins();

            String seedImage = ecoConfig.getGalasaRegistry() + "/galasa-seed-amd64:" + ecoConfig.getSeedVersion();
            String imageId = getImageId(seedImage);
            
            
            CreateContainerCmd cmd = dockerClient.createContainerCmd(RESOURCE_NAME);
            cmd.withName(RESOURCE_NAME);
            cmd.withImage(imageId);

            HostConfig hostConfig = new HostConfig();
            Mount mount = new Mount();
            mount.withType(MountType.VOLUME);
            mount.withSource(RESOURCE_NAME);
            mount.withTarget("/var/jenkins_home");
            ArrayList<Mount> mounts = new ArrayList<>();
            mounts.add(mount);

            hostConfig.withMounts(mounts);
            cmd.withHostConfig(hostConfig);
            
            cmd.withCmd("sh","/jenkins.sh", 
                    ecoConfig.getHostname(), 
                    Integer.toString(jenkinsConfig.getPort()),
                    Integer.toString(this.api.getPort()));
            CreateContainerResponse createResponse = cmd.exec();
            
            System.out.println("Starting Jenkins volume seeding container");
            StartContainerCmd startCmd = dockerClient.startContainerCmd(createResponse.getId());
            startCmd.exec();
            
            WaitContainerResultCallback callback = new WaitContainerResultCallback();
            dockerClient.waitContainerCmd(createResponse.getId()).exec(callback);
            callback.awaitCompletion(1, TimeUnit.MINUTES);
            System.out.println("Deleteing Jenkins volume seeding container");
            dockerClient.removeContainerCmd(createResponse.getId()).withForce(true).exec();

            System.out.println("Jenkins volume created");
        } catch(Exception e) {
            try {
                InspectContainerResponse response = dockerClient.inspectContainerCmd(RESOURCE_NAME).exec();
                dockerClient.removeContainerCmd(response.getId()).withForce(true).exec();
            } catch(NotFoundException e1) {
            } catch(Exception e1) {
                System.out.println("Clean up of partial Jenkins volume seed container failed");
                e1.printStackTrace();
            }
            try {
                dockerClient.removeVolumeCmd(RESOURCE_NAME).exec();
            } catch(NotFoundException e1) {
            } catch(Exception e1) {
                System.out.println("Clean up of partial Jenkins volume failed");
                e1.printStackTrace();
            }
            throw new DockerOperatorException("Problem creating the Jenkins volume", e);
        }
    }

    public String getName() {
        return RESOURCE_NAME;
    }

}
