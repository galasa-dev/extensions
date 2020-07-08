/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.model;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Properties;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;

import dev.galasa.docker.operator.DockerOperatorException;
import dev.galasa.docker.operator.config.EcosystemConfiguration;
import dev.galasa.docker.operator.config.EngineConfiguration;

public class EngineController extends AbstractContainerResource {

    private static final String RESOURCE_NAME = "galasa_engine_controller";

    private final Cps               cps;
    private final Ras               ras;
    private final Api               api;

    public EngineController(Ecosystem ecosystem) {
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
        String targetImageName = getControllerImageName();

        DockerClient dockerClient = getEcosystem().getDockerClient();
        
        String imageId = null;
        try {
            imageId = getImageId(targetImageName);
        } catch(Exception e) {
            throw new DockerOperatorException("Problem determining ENGINE image id", e);
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
            throw new DockerOperatorException("Problem inspecting ENGINE container", e);
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

        Path tempDir = null; 

        try {
            tempDir = Files.createTempDirectory("galasadockeroperator");

            System.out.println("Defining the ENGINE container");
            CreateContainerCmd cmd = dockerClient.createContainerCmd(RESOURCE_NAME);
            cmd.withName(RESOURCE_NAME);
            cmd.withImage(targetImageName);
 
            HostConfig hostConfig = new HostConfig();
            cmd.withHostConfig(hostConfig);
            
            URI dockerHost = getEcosystem().getDockerHost();
            if (dockerHost.getScheme().equals("unix")) {
                Mount mount = new Mount();
                mount.withType(MountType.BIND);
                mount.withSource(dockerHost.toString().substring(5));
                mount.withTarget(dockerHost.toString().substring(5));
                ArrayList<Mount> mounts = new ArrayList<>();
                mounts.add(mount);
                hostConfig.withMounts(mounts);
            }
            
            cmd.withEnv("DOCKERHOST=" + dockerHost.toString());
            cmd.withUser("root"); 
            
            cmd.withCmd("java",
                    "-jar",
                    "boot.jar",
                    "--obr",
                    "file:galasa.obr",
                    "--trace",
                    "--dockercontroller",
                    "--bootstrap",
                    api.getBootstrap());

            cmd.exec();
            
            Properties config = new Properties();
            config.setProperty("bootstrap", this.api.getBootstrap());
            config.setProperty("engine_image", getEngineImageName());
            config.setProperty("max_engines",Integer.toString(getEcosystem().getConfiguration().getEngineController().getMaxEngines()));

            Path bootstrap = tempDir.resolve("galasa.properties");
            config.store(Files.newOutputStream(bootstrap, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), null);

            putFile("/etc/", bootstrap.toFile());
        } catch(Exception e) {
            throw new DockerOperatorException("Problem creating ENGINE container", e);
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
        System.out.println("ENGINE Container is down, requires start up");

        for(AbstractResource dependency : getDependencies()) {
            dependency.dependencyChanging(DependencyEvent.STARTING, this);
        }

        try {
            System.out.println("Starting ENGINE container");
            startContainer();
        } catch(Exception e) {
            throw new DockerOperatorException("Problem starting ENGINE container", e);
        }

        try {
            checkLog("Docker controller has started", 120);
            System.out.println("ENGINE container is up");
        } catch(Exception e) {
            System.out.println("Failed to detect ENGINE up message, deleting container to force rebuild");
            deleteContainer();
            throw new DockerOperatorException("Problem waiting for ENGINE container started message", e);
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
                        System.out.println("CPS container status is changing, stopping ENGINE");
                    }
                    stopContainer();
                    return;
                }
                if (resource == this.ras) {
                    if (this.isContainerRunning(false)) {
                        System.out.println("RAS container status is changing, stopping ENGINE");
                    }
                    stopContainer();
                    return;
                }
                if (resource == this.api) {
                    if (this.isContainerRunning(false)) {
                        System.out.println("API container status is changing, stopping ENGINE");
                    }
                    stopContainer();
                    return;
                }
                break;
            default:
                throw new DockerOperatorException("Unexpected event '" + event + " from dependency " + resource.getClass().getName());
        }
    }


    private String getControllerImageName() {
        EcosystemConfiguration ecoConfig = getEcosystem().getConfiguration();
        EngineConfiguration engineConfig = ecoConfig.getEngineController();
        
        String controllerImage = engineConfig.getControllerImage();
        String controllerVersion = engineConfig.getControllerVersion();
        
        if (controllerVersion == null || controllerVersion.isEmpty()) {
            controllerVersion = ecoConfig.getVersion();
        }
        
        
        return ecoConfig.getGalasaRegistry() + "/" + controllerImage + ":" + controllerVersion;
    }

    private String getEngineImageName() {
        EcosystemConfiguration ecoConfig = getEcosystem().getConfiguration();
        EngineConfiguration engineConfig = ecoConfig.getEngineController();
        
        String engineImage = engineConfig.getEngineImage();
        String engineVersion = engineConfig.getEngineVersion();
        
        if (engineVersion == null || engineVersion.isEmpty()) {
            engineVersion = ecoConfig.getVersion();
        }
        
        
        return ecoConfig.getGalasaRegistry() + "/" + engineImage + ":" + engineVersion;
    }


}
