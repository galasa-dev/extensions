/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.model;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;

import dev.galasa.docker.operator.DockerOperatorException;
import dev.galasa.docker.operator.config.EcosystemConfiguration;
import dev.galasa.docker.operator.config.ResMonConfiguration;

public class ResMon extends AbstractContainerResource {

    private static final String RESOURCE_NAME = "galasa_resource_management";

    private final Cps               cps;
    private final Ras               ras;
    private final Api               api;

    public ResMon(Ecosystem ecosystem) {
        super(ecosystem, RESOURCE_NAME);

        this.cps = ecosystem.getResource(Cps.class);
        this.cps.addDependency(this);

        this.ras = ecosystem.getResource(Ras.class);
        this.ras.addDependency(this);

        this.api = ecosystem.getResource(Api.class);
        this.api.addDependency(this);
    }

    @Override
    public void checkResourceDefined() throws DockerOperatorException {
        String targetImageName = getTargetImageName();

        DockerClient dockerClient = getEcosystem().getDockerClient();

        String imageId = null;
        try {
            imageId = getImageId(targetImageName);
        } catch(Exception e) {
            throw new DockerOperatorException("Problem determining RESMON image id", e);
        }

        boolean found   = true;
        boolean correct = true;
        try {
            InspectContainerResponse response = dockerClient.inspectContainerCmd(RESOURCE_NAME).exec();

            String actualImage = response.getImageId();
            if (!imageId.equals(actualImage)) {
                correct = false;
            }

        } catch(NotFoundException e) {
            found = false;
        } catch(Exception e) {
            throw new DockerOperatorException("Problem inspecting RESMON container", e);
        }

        if (found && correct) {
            return;
        }

        for(AbstractResource dependency : getDependencies()) {
            dependency.dependencyChanging(DependencyEvent.DEFINING, this);
        }

        if (!correct) {
            stopContainer();
            deleteContainer();
        }

        try {
            System.out.println("Defining the RESMON container");
            CreateContainerCmd cmd = dockerClient.createContainerCmd(RESOURCE_NAME);
            cmd.withName(RESOURCE_NAME);
            cmd.withImage(targetImageName);

            cmd.withCmd("java",
                    "-jar",
                    "boot.jar",
                    "--obr",
                    "file:galasa.obr",
                    "--trace",
                    "--resourcemanagement",
                    "--bootstrap",
                    api.getBootstrap());

            cmd.exec();

        } catch(Exception e) {
            throw new DockerOperatorException("Problem creating RESMON container", e);
        }
    }


    @Override
    protected void checkResourceRunning() throws DockerOperatorException {
        checkContainerRunning();
    }


    protected void checkContainerRunning() throws DockerOperatorException {
        if (isContainerRunning(false)) {
            return;
        }
        System.out.println("RESMON Container is down, requires start up");

        for(AbstractResource dependency : getDependencies()) {
            dependency.dependencyChanging(DependencyEvent.STARTING, this);
        }

        try {
            System.out.println("Starting RESMON container");
            startContainer();
        } catch(Exception e) {
            throw new DockerOperatorException("Problem starting RESMON container", e);
        }

        try {
            checkLog("Resource Manager has started", 120);
            System.out.println("RESMON container is up");
        } catch(Exception e) {
            System.out.println("Failed to detect RESMON up message, deleting container to force rebuild");
            deleteContainer();
            throw new DockerOperatorException("Problem waiting for RESMON container started message", e);
        }
    }



    @Override
    protected void dependencyChanging(DependencyEvent event, AbstractResource resource) throws DockerOperatorException {
        switch(event) {
            case DEFINING:
            case STARTING:
            case STOPPING:
                if (resource == this.cps) {
                    if (this.isContainerRunning(false)) {
                        System.out.println("CPS container status is changing, stopping RESMON");
                    }
                    stopContainer();
                    return;
                }
                if (resource == this.ras) {
                    if (this.isContainerRunning(false)) {
                        System.out.println("RAS container status is changing, stopping RESMON");
                    }
                    stopContainer();
                    return;
                }
                if (resource == this.api) {
                    if (this.isContainerRunning(false)) {
                        System.out.println("API container status is changing, stopping RESMON");
                    }
                    stopContainer();
                    return;
                }
                break;
            default:
                throw new DockerOperatorException("Unexpected event '" + event + " from dependency " + resource.getClass().getName());
        }
    }


    private String getTargetImageName() {
        EcosystemConfiguration ecoConfig = getEcosystem().getConfiguration();
        ResMonConfiguration resmonConfig = ecoConfig.getResourceManagement();
        return ecoConfig.getGalasaRegistry() + "/" + resmonConfig.getImage() + ":" + ecoConfig.getVersion();
    }


}
