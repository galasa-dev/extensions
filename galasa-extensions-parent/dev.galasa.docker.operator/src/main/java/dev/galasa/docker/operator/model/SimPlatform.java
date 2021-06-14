/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020-2021.
 */
package dev.galasa.docker.operator.model;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

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
import dev.galasa.docker.operator.config.SimPlatformConfiguration;

public class SimPlatform extends AbstractContainerResource {

    private static final String RESOURCE_NAME = "galasa_simplatform";
    
    private final Cps cps;
    private final Api api;
    private final Nexus nexus;

    public SimPlatform(Ecosystem ecosystem) {
        super(ecosystem, RESOURCE_NAME);
        
        this.cps = ecosystem.getResource(Cps.class);
        this.cps.addDependency(this);
        
        this.api = ecosystem.getResource(Api.class);
        this.api.addDependency(this);
        
        this.nexus = ecosystem.getResource(Nexus.class);
        this.nexus.addDependency(this);
    }

    @Override
    public void checkResourceDefined() throws DockerOperatorException {
        String targetImageName = getTargetImageName();

        DockerClient dockerClient = getEcosystem().getDockerClient();

        String imageId = null;
        try {
            imageId = getImageId(targetImageName);
        } catch(Exception e) {
            throw new DockerOperatorException("Problem determining SIMPLATFORM image id", e);
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
            throw new DockerOperatorException("Problem inspecting SIMPLATFORM container", e);
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
            System.out.println("Defining the SIMPLATFORM container");
            CreateContainerCmd cmd = dockerClient.createContainerCmd(RESOURCE_NAME);
            cmd.withName(RESOURCE_NAME);
            cmd.withImage(targetImageName);
            
            HostConfig hostConfig = new HostConfig();
            cmd.withHostConfig(hostConfig);
            
            SimPlatformConfiguration simConfig = getEcosystem().getConfiguration().getSimplatform();
            
            hostConfig.withPortBindings(
                    new PortBinding(new Binding("0.0.0.0", Integer.toString(simConfig.getTelnetPort())), new ExposedPort(2023)),
                    new PortBinding(new Binding("0.0.0.0", Integer.toString(simConfig.getWebservicePort())), new ExposedPort(2080)),
                    new PortBinding(new Binding("0.0.0.0", Integer.toString(simConfig.getDatabasePort())), new ExposedPort(2027)),
                    new PortBinding(new Binding("0.0.0.0", Integer.toString(simConfig.getZosmfPort())), new ExposedPort(2040)));

            cmd.withCmd("java",
                    "-jar",
                    "simplatform.jar");

            cmd.exec();

        } catch(Exception e) {
            throw new DockerOperatorException("Problem creating SIMPLATFORM container", e);
        }
    }


    @Override
    protected void checkResourceRunning() throws DockerOperatorException {
        checkContainerRunning();
        checkTestCatalog();
        checkTestStream();
        checkDse();
    }

    private void checkTestStream() throws DockerOperatorException {

        String location    = this.api.getTestcatalog() + "/simbank";
        String obr         = "mvn:dev.galasa/dev.galasa.simbank.obr/" + getEcosystem().getConfiguration().getVersion() + "/obr";
        String description = "SimBank Tests";
        String repo        = "http://" + getEcosystem().getConfiguration().getHostname() + ":" + this.nexus.getPort() + "/repository/maven";

        try {
            this.cps.checkCpsProperty("framework.test.stream.SIMBANK.description", description);
            this.cps.checkCpsProperty("framework.test.stream.SIMBANK.location", location);
            this.cps.checkCpsProperty("framework.test.stream.SIMBANK.obr", obr);
            this.cps.checkCpsProperty("framework.test.stream.SIMBANK.repo", repo);
            
            this.cps.checkCpsProperty("framework.test.streams", "SIMBANK", false);
        } catch(DockerOperatorException e) {
            throw new DockerOperatorException("Unable to check or set the framework.test.stream.simbank.* properties", e);
        }
    }


    private void checkDse() throws DockerOperatorException {
        
        EcosystemConfiguration ecoConfig = getEcosystem().getConfiguration();
        SimPlatformConfiguration simConfig = ecoConfig.getSimplatform();
               
        try {
            this.cps.checkCpsProperty("zos.dse.tag.SIMBANK.imageid", "SIMBANK");
            this.cps.checkCpsProperty("zos.dse.tag.SIMBANK.clusterid", "SIMBANK");
            
            this.cps.checkCpsProperty("simbank.dse.instance.name", "SIMBANK");
            this.cps.checkCpsProperty("simbank.dse.SIMBANK.zos.image", "SIMBANK");

            this.cps.checkCpsProperty("zos.image.SIMBANK.ipv4.hostname", ecoConfig.getHostname());
            this.cps.checkCpsProperty("zos.image.SIMBANK.telnet.port", Integer.toString(simConfig.getTelnetPort()));
            this.cps.checkCpsProperty("zos.image.SIMBANK.webnet.port", Integer.toString(simConfig.getWebservicePort()));
            this.cps.checkCpsProperty("zos.image.SIMBANK.database.port", Integer.toString(simConfig.getDatabasePort()));
            this.cps.checkCpsProperty("zos.image.SIMBANK.telnet.tls", "false");
            this.cps.checkCpsProperty("zos.image.SIMBANK.credentials", "SIMBANK");
            
            this.cps.checkCpsProperty("zosmf.image.SIMBANK.servers", "SIMBANK");
            this.cps.checkCpsProperty("zosmf.server.SIMBANK.image", "SIMBANK");
            this.cps.checkCpsProperty("zosmf.server.SIMBANK.port", Integer.toString(simConfig.getZosmfPort()));
            this.cps.checkCpsProperty("zosmf.server.SIMBANK.https", "false");

            this.cps.checkCpsProperty("secure.credentials.SIMBANK.username", "IBMUSER");
            this.cps.checkCpsProperty("secure.credentials.SIMBANK.password", "SYS1");
        } catch(DockerOperatorException e) {
            throw new DockerOperatorException("Unable to check or set the framework.test.stream.simbank.* properties", e);
        }
    }


    private void checkTestCatalog() throws DockerOperatorException {
        try {
            URL testcatalogs = new URL(this.api.getTestcatalog() + "/simbank");
            
            HttpURLConnection conn = (HttpURLConnection) testcatalogs.openConnection();
            Object o = conn.getContent();
            if (o instanceof InputStream) {
                String json = IOUtils.toString((InputStream)o, StandardCharsets.UTF_8);
                
                if (json.contains("\"name\": \"Galasa SimBank OBR\"")) {
                    return;
                }
                
                System.out.println("SimBank test catalog is invalid");
            }
        } catch(FileNotFoundException e) {
            System.out.println("SimBank test catalog is missing");
        } catch(Exception e) {
            throw new DockerOperatorException("Problem checking the SimBank test catalog",e);
        }
        
        System.out.println("Creating the SimBank test catalog");
        
        try {
            URL testcatalogs = new URL(this.api.getTestcatalog() + "/simbank");
            
            InputStream catalog = getClass().getResourceAsStream("/simplatform-testcatalog.json");            
            
            
            HttpURLConnection conn = (HttpURLConnection) testcatalogs.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty(
                    "Content-Type", "application/json" );
            IOUtils.copy(catalog, conn.getOutputStream());
            IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8);
        } catch(FileNotFoundException e) {
            System.out.println("SimBank test catalog is missing");
        } catch(Exception e) {
            throw new DockerOperatorException("Problem checking the SimBank test catalog",e);
        }
        
        System.out.println("SimBank test catalog stored");
        
    }

    protected void checkContainerRunning() throws DockerOperatorException {
        if (isContainerRunning(false)) {
            return;
        }
        System.out.println("SIMPLATFORM Container is down, requires start up");

        for(AbstractResource dependency : getDependencies()) {
            dependency.dependencyChanging(DependencyEvent.STARTING, this);
        }

        try {
            System.out.println("Starting SIMPLATFORM container");
            startContainer();
        } catch(Exception e) {
            throw new DockerOperatorException("Problem starting SIMPLATFORM container", e);
        }

        try {
            checkLog("Simplatform started", 120);
            System.out.println("SIMPLATFORM container is up");
        } catch(Exception e) {
            System.out.println("Failed to detect SIMPLATFORM up message, deleting container to force rebuild");
            deleteContainer();
            throw new DockerOperatorException("Problem waiting for SIMPLATFORM container started message", e);
        }
    }



    @Override
    protected void dependencyChanging(DependencyEvent event, AbstractResource resource) throws DockerOperatorException {
        switch(event) {
            case DEFINING:
                stopContainer();
                deleteContainer();
                break;
            case STARTING:
            case STOPPING:
                stopContainer();
                return;
            default:
                throw new DockerOperatorException("Unexpected event '" + event + " from dependency " + resource.getClass().getName());
        }
    }


    private String getTargetImageName() {
        EcosystemConfiguration ecoConfig = getEcosystem().getConfiguration();
        SimPlatformConfiguration simplatformConfig = ecoConfig.getSimplatform();
        String version = simplatformConfig.getVersion();
        if (version == null || version.isEmpty()) {
            version = ecoConfig.getVersion();
        }
        
        return ecoConfig.getGalasaRegistry() + "/" + simplatformConfig.getImage() + ":" + version;
    }


}
