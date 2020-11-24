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
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;

import dev.galasa.docker.operator.DockerOperatorException;
import dev.galasa.docker.operator.config.EcosystemConfiguration;
import dev.galasa.docker.operator.config.MetricsConfiguration;

public class Metrics extends AbstractContainerResource {

    private static final String RESOURCE_NAME = "galasa_metrics";

    private final Cps               cps;
    private final Ras               ras;
    private final Api               api;

    public Metrics(Ecosystem ecosystem) {
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
            throw new DockerOperatorException("Problem determining METRICS image id", e);
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
            throw new DockerOperatorException("Problem inspecting METRICS container", e);
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
            System.out.println("Defining the METRICS container");
            CreateContainerCmd cmd = dockerClient.createContainerCmd(RESOURCE_NAME);
            cmd.withName(RESOURCE_NAME);
            cmd.withImage(targetImageName);
            
            HostConfig hostConfig = new HostConfig();
            cmd.withHostConfig(hostConfig);
            
            MetricsConfiguration metricsConfig = getEcosystem().getConfiguration().getMetrics();
            
            hostConfig.withPortBindings(
                    new PortBinding(new Binding("0.0.0.0", Integer.toString(metricsConfig.getPort())), new ExposedPort(9010)));


            cmd.withCmd("java",
                    "-jar",
                    "boot.jar",
                    "--obr",
                    "file:galasa.obr",
                    "--trace",
                    "--metricserver",
                    "--bootstrap",
                    api.getBootstrap());

            cmd.exec();

        } catch(Exception e) {
            throw new DockerOperatorException("Problem creating METRICS container", e);
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
        System.out.println("METRICS Container is down, requires start up");

        for(AbstractResource dependency : getDependencies()) {
            dependency.dependencyChanging(DependencyEvent.STARTING, this);
        }

        try {
            System.out.println("Starting METRICS container");
            startContainer();
        } catch(Exception e) {
            throw new DockerOperatorException("Problem starting METRICS container", e);
        }

        try {
            checkLog("Metrics Server has started", 120);
            System.out.println("METRICS container is up");
        } catch(Exception e) {
            System.out.println("Failed to detect METRICS up message, deleting container to force rebuild");
            deleteContainer();
            throw new DockerOperatorException("Problem waiting for METRICS container started message", e);
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
                        System.out.println("CPS container status is changing, stopping METRICS");
                    }
                    stopContainer();
                    return;
                }
                if (resource == this.ras) {
                    if (this.isContainerRunning(false)) {
                        System.out.println("RAS container status is changing, stopping METRICS");
                    }
                    stopContainer();
                    return;
                }
                if (resource == this.api) {
                    if (this.isContainerRunning(false)) {
                        System.out.println("API container status is changing, stopping METRICS");
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
        MetricsConfiguration metricsConfig = ecoConfig.getMetrics();
        return ecoConfig.getGalasaRegistry() + "/" + metricsConfig.getImage() + ":" + ecoConfig.getVersion();
    }


}
