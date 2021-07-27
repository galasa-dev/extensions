/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020-2021.
 */
package dev.galasa.docker.operator.model;

import java.net.URI;
import java.util.ArrayList;

import com.github.dockerjava.api.DockerClient;

import dev.galasa.docker.operator.DockerOperatorException;
import dev.galasa.docker.operator.config.EcosystemConfiguration;

public class Ecosystem {
    
    private final ArrayList<AbstractResource> resources = new ArrayList<>();
    
    private final EcosystemConfiguration configuration;
    private final DockerClient           dockerClient;
    private final URI                    dockerHost;

    public Ecosystem(EcosystemConfiguration configuration, DockerClient dockerClient, URI dockerHost) {
        this.configuration = configuration;
        this.dockerClient  = dockerClient;
        this.dockerHost    = dockerHost;
        
        this.resources.add(new Cps(this));
        this.resources.add(new Ras(this));
        this.resources.add(new Api(this));
        this.resources.add(new ResMan(this));
        this.resources.add(new Metrics(this));
        this.resources.add(new EngineController(this));
        this.resources.add(new Nexus(this));
        this.resources.add(new SimPlatform(this));
    }

    public void buildDependencies() {
        // TODO Auto-generated method stub
        
    }

    public void buildResources() {
        // TODO Auto-generated method stub
        
    }

    public void addResource(AbstractResource resource) {
        this.resources.add(resource);
    }
    
    public EcosystemConfiguration getConfiguration() {
        return this.configuration;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends AbstractResource> T getResource(Class<T> resourceType) {
        for(AbstractResource resource : resources) {
            if (resource.getClass().isAssignableFrom(resourceType)) {
                return (T)resource;
            }
        }
        return null;
    }

    public void checkResourceDefined() throws DockerOperatorException {
        for(AbstractResource resource : resources) {
            resource.checkResourceDefined();
        }
    }

    public void checkResourcesRunning() throws DockerOperatorException {
        for(AbstractResource resource : resources) {
            resource.checkResourceRunning();
        }
    }

    public DockerClient getDockerClient() {
        return this.dockerClient;
    }
    
    public URI getDockerHost() {
        return this.dockerHost;
    }

}
