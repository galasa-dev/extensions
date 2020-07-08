/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;

import dev.galasa.docker.operator.DockerOperatorException;
import dev.galasa.docker.operator.config.ApiConfiguration;
import dev.galasa.docker.operator.config.EcosystemConfiguration;

public class Api extends AbstractContainerResource {

    private static final String RESOURCE_NAME = "galasa_api";

    private final TestcatalogVolume testcatalogVolume;
    private final Cps               cps;
    private final Ras               ras;

    public Api(Ecosystem ecosystem) {
        super(ecosystem, RESOURCE_NAME);

        this.testcatalogVolume = new TestcatalogVolume(ecosystem);
        this.testcatalogVolume.addDependency(this);
        ecosystem.addResource(this.testcatalogVolume);

        this.cps = ecosystem.getResource(Cps.class);
        this.cps.addDependency(this);

        this.ras = ecosystem.getResource(Ras.class);
        this.ras.addDependency(this);
    }

    @Override
    public void checkResourceDefined() throws DockerOperatorException {
        this.testcatalogVolume.checkResourceDefined();

        String rasVolumeName = this.testcatalogVolume.getName();
        String targetImageName = getTargetImageName();

        DockerClient dockerClient = getEcosystem().getDockerClient();

        String imageId = null;
        try {
            imageId = getImageId(targetImageName);
        } catch(Exception e) {
            throw new DockerOperatorException("Problem determining API image id", e);
        }

        boolean found   = true;
        boolean correct = true;
        try {
            InspectContainerResponse response = dockerClient.inspectContainerCmd(RESOURCE_NAME).exec();

            String actualImage = response.getImageId();
            if (!imageId.equals(actualImage)) {
                correct = false;
            }

            if (!checkVolumePresent(response.getMounts(), rasVolumeName, "/galasa/testcatalog")) {
                correct = false;
            }

            //            String bootstrap = getFile("/bootstrap.properties");
            //            System.out.println(bootstrap);

        } catch(NotFoundException e) {
            found = false;
        } catch(Exception e) {
            throw new DockerOperatorException("Problem inspecting API container", e);
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

            System.out.println("Defining the API container");
            CreateContainerCmd cmd = dockerClient.createContainerCmd(RESOURCE_NAME);
            cmd.withName(RESOURCE_NAME);
            cmd.withImage(targetImageName);

            HostConfig hostConfig = new HostConfig();
            String portNumber = Integer.toString(getEcosystem().getConfiguration().getApi().getPort());
            hostConfig.withPortBindings(new PortBinding(new Binding("0.0.0.0", portNumber), new ExposedPort(8080)));

            Mount mount = new Mount();
            mount.withType(MountType.VOLUME);
            mount.withSource(rasVolumeName);
            mount.withTarget("/galasa/testcatalog");
            ArrayList<Mount> mounts = new ArrayList<>();
            mounts.add(mount);

            hostConfig.withMounts(mounts);
            cmd.withHostConfig(hostConfig);

            cmd.withCmd("java",
                    "-jar",
                    "boot.jar",
                    "--obr",
                    "file:galasa.obr",
                    "--trace",
                    "--api",
                    "--bootstrap",
                    "file:/bootstrap.properties");

            cmd.exec();

            StringBuilder sb = new StringBuilder();
            sb.append("framework.config.store=");
            sb.append(this.cps.getLocation());
            sb.append("\n");

            sb.append("framework.extra.bundles=");
            sb.append(getEcosystem().getConfiguration().getApi().getExtraBundles());
            sb.append("\n");

            Path bootstrap = tempDir.resolve("bootstrap.properties");
            Files.write(bootstrap, sb.toString().getBytes());

            putFile("/", bootstrap.toFile());

            Path testcatalog = tempDir.resolve("dev.galasa.testcatalog.cfg");
            Files.write(testcatalog, "framework.testcatalog.directory=file:/galasa/testcatalog".getBytes());
            putFile("/galasa/load", testcatalog.toFile());
        } catch(Exception e) {
            throw new DockerOperatorException("Problem creating API container", e);
        } finally {
            try {
                if (tempDir != null) {
                    FileUtils.deleteDirectory(tempDir.toFile());
                }
            } catch (IOException e) {
            }
        }
    }


    private boolean checkVolumePresent(List<com.github.dockerjava.api.command.InspectContainerResponse.Mount> mounts, String volumeName, String path) {

        if (mounts == null || mounts.isEmpty()) {
            return false;
        }


        for(com.github.dockerjava.api.command.InspectContainerResponse.Mount mount : mounts) {
            if (mount.getName().equals(volumeName)) {
                if (mount.getDestination().getPath().equals(path)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void checkResourceRunning() throws DockerOperatorException {
        checkContainerRunning();

        try {
            checkBootstrapResponding();
            checkTestcatalogResponding();
        } catch(Exception e) {
            System.out.println("Problem accessing API services, deleting container to force rebuild");
            deleteContainer();
            throw new DockerOperatorException("Problem with API services",e);
        }
    }


    private void checkBootstrapResponding() throws DockerOperatorException {
        String bootstrap = getBootstrap();

        Instant expire = Instant.now().plus(1, ChronoUnit.MINUTES);
        while(Instant.now().isBefore(expire)) {
            try {
                URL url = new URL(bootstrap);
                Object o = url.getContent();
                if (o instanceof InputStream) {
                    Properties properties = new Properties();
                    properties.load((InputStream)o);
                    
                    if (properties.containsKey("framework.config.store")) {
                        return;
                    }
                }
            } catch(Exception e) {
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DockerOperatorException("Delay interrupted");
            }
        }

        throw new DockerOperatorException("Unable to contact API Bootstrap service in time");

    }

    private void checkTestcatalogResponding() throws DockerOperatorException {
        String testcatalogs = getTestcatalog();
        
        Instant expire = Instant.now().plus(1, ChronoUnit.MINUTES);
        while(Instant.now().isBefore(expire)) {
            try {
                URL url = new URL(testcatalogs);
                Object o = url.getContent();
                if (o instanceof InputStream) {
                    String json = IOUtils.toString((InputStream)o, StandardCharsets.UTF_8);
                    
                    if (json.contains("\"catalogs\":")) {
                        return;
                    }
                }
            } catch(Exception e) {
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DockerOperatorException("Delay interrupted");
            }
        }

        throw new DockerOperatorException("Unable to contact API Bootstrap service in time");

    }

    protected void checkContainerRunning() throws DockerOperatorException {
        if (isContainerRunning(false)) {
            return;
        }
        System.out.println("API Container is down, requires start up");

        for(AbstractResource dependency : getDependencies()) {
            dependency.dependencyChanging(DependencyEvent.STARTING, this);
        }

        try {
            System.out.println("Starting API container");
            startContainer();
        } catch(Exception e) {
            throw new DockerOperatorException("Problem starting API container", e);
        }
        
        try {
            StringBuffer sbStdout = new StringBuffer();
            StringBuffer sbStderr = new StringBuffer();
            issueCommand(sbStdout, sbStderr, 10, "chown","galasa", "/galasa/testcatalog");
            
            String stdout = sbStdout.toString().trim();
            String stderr = sbStderr.toString().trim();
            
            if (!stdout.isEmpty() || !stderr.isEmpty()) {
                throw new DockerOperatorException("Unrecognised output frpm chown:-\n" + stdout.toString() + "\n" + stderr.toString());
            }
        } catch(Exception e) {
            System.out.println("Failed to change ownership of test catalog directory, deleting container to force rebuild");
            deleteContainer();
            throw new DockerOperatorException("Failed to change ownership of test catalog directory", e);
        }

        try {
            checkLog("API server has started", 120);
            checkLog("Galasa Test Catalog activated", 120);
            System.out.println("API container is up");
        } catch(Exception e) {
            System.out.println("Failed to detect API up message, deleting container to force rebuild");
            deleteContainer();
            throw new DockerOperatorException("Problem waiting for API container started message", e);
        }
    }



    @Override
    protected void dependencyChanging(DependencyEvent event, AbstractResource resource) throws DockerOperatorException {
        switch(event) {
            case DEFINING:
            case STARTING:
            case STOPPING:
                if (resource == this.testcatalogVolume) {
                    stopContainer();
                    deleteContainer();
                    return;
                }
                if (resource == this.cps) {
                    if (this.isContainerRunning(false)) {
                        System.out.println("CPS container status is changing, stopping API");
                    }
                    stopContainer();
                    return;
                }
                if (resource == this.ras) {
                    if (this.isContainerRunning(false)) {
                        System.out.println("RAS container status is changing, stopping API");
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
        ApiConfiguration apiConfig = ecoConfig.getApi();
        return ecoConfig.getGalasaRegistry() + "/" + apiConfig.getImage() + ":" + ecoConfig.getVersion();
    }

    public String getBootstrap() {
        EcosystemConfiguration ecoConfig = getEcosystem().getConfiguration();
        ApiConfiguration apiConfig = ecoConfig.getApi();
        return "http://" + ecoConfig.getHostname() + ":" + apiConfig.getPort() + "/bootstrap";
    }

    public String getTestcatalog() {
        EcosystemConfiguration ecoConfig = getEcosystem().getConfiguration();
        ApiConfiguration apiConfig = ecoConfig.getApi();
        return "http://" + ecoConfig.getHostname() + ":" + apiConfig.getPort() + "/testcatalog";
    }

    public int getPort() {
        EcosystemConfiguration ecoConfig = getEcosystem().getConfiguration();
        ApiConfiguration apiConfig = ecoConfig.getApi();
        return apiConfig.getPort();
    }


}
