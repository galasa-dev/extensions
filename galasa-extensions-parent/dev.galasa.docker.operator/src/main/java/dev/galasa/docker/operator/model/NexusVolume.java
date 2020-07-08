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

public class NexusVolume extends AbstractResource {

    private static final String RESOURCE_NAME = "galasa_nexus_volume";

    public NexusVolume(Ecosystem ecosystem) {
        super(ecosystem);
    }

    @Override
    public void checkResourceDefined() throws DockerOperatorException {

        DockerClient dockerClient = getEcosystem().getDockerClient();

        try {
            dockerClient.inspectVolumeCmd(RESOURCE_NAME).exec();

            return;
        } catch(NotFoundException e) {
            System.out.println("Nexus volume is missing");
        } catch(Exception e) {
            throw new DockerOperatorException("Problem inspecting the Nexus volume resource", e);
        }

        for(AbstractResource resource : getDependencies()) {
            resource.dependencyChanging(DependencyEvent.DEFINING, this);
        }

        try {
            System.out.println("Creating Nexus volume seeding container");
            EcosystemConfiguration ecoConfig = getEcosystem().getConfiguration();
            String seedImage = ecoConfig.getGalasaRegistry() + "/galasa-seed-amd64:" + ecoConfig.getVersion();
            String imageId = getImageId(seedImage);
            
            
            CreateContainerCmd cmd = dockerClient.createContainerCmd(RESOURCE_NAME);
            cmd.withName(RESOURCE_NAME);
            cmd.withImage(imageId);

            HostConfig hostConfig = new HostConfig();
            Mount mount = new Mount();
            mount.withType(MountType.VOLUME);
            mount.withSource(RESOURCE_NAME);
            mount.withTarget("/nexus-data");
            ArrayList<Mount> mounts = new ArrayList<>();
            mounts.add(mount);

            hostConfig.withMounts(mounts);
            cmd.withHostConfig(hostConfig);
            
            cmd.withCmd("sh","/nexus.sh");
            CreateContainerResponse createResponse = cmd.exec();
            
            System.out.println("Starting Nexus volume seeding container");
            StartContainerCmd startCmd = dockerClient.startContainerCmd(createResponse.getId());
            startCmd.exec();
            
            WaitContainerResultCallback callback = new WaitContainerResultCallback();
            dockerClient.waitContainerCmd(createResponse.getId()).exec(callback);
            callback.awaitCompletion(1, TimeUnit.MINUTES);
            System.out.println("Deleteing Nexus volume seeding container");
            dockerClient.removeContainerCmd(createResponse.getId()).withForce(true).exec();

            System.out.println("Nexus volume created");
        } catch(Exception e) {
            try {
                InspectContainerResponse response = dockerClient.inspectContainerCmd(RESOURCE_NAME).exec();
                dockerClient.removeContainerCmd(response.getId()).withForce(true).exec();
            } catch(NotFoundException e1) {
            } catch(Exception e1) {
                System.out.println("Clean up of partial nexus volume seed container failed");
                e1.printStackTrace();
            }
            try {
                dockerClient.removeVolumeCmd(RESOURCE_NAME).exec();
            } catch(NotFoundException e1) {
            } catch(Exception e1) {
                System.out.println("Clean up of partial nexus volume failed");
                e1.printStackTrace();
            }
            throw new DockerOperatorException("Problem creating the Nexus volume", e);
        }
    }

    public String getName() {
        return RESOURCE_NAME;
    }

}
