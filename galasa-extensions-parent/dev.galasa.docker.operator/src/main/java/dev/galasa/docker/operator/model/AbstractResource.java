/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;

import dev.galasa.docker.operator.DockerOperatorException;

public abstract class AbstractResource {
    
    private final Ecosystem ecosystem;    
    private List<AbstractResource> dependentResources = new ArrayList<>();
    
    public AbstractResource(Ecosystem ecosystem) {
        this.ecosystem = ecosystem;
    }

    protected void addDependency(AbstractResource resource) {
        this.dependentResources.add(resource);
    }
    
    public Ecosystem getEcosystem() {
        return this.ecosystem;
    }

    public List<AbstractResource> getDependencies() {
        return this.dependentResources;
    }

    public void checkResourceDefined() throws DockerOperatorException { // TODO abstract
    }
    
    public enum DependencyEvent {
        DEFINING,
        STARTING,
        STOPPING
    }
    
    protected void dependencyChanging(DependencyEvent event, AbstractResource resource) throws DockerOperatorException { // TODO abstract
    }

    protected void checkResourceRunning() throws DockerOperatorException {// TODO abstract
        
    }
    
    protected String getImageId(String imageName) throws DockerOperatorException {
        DockerClient dockerClient = getEcosystem().getDockerClient();

        try {
            InspectImageResponse response = dockerClient.inspectImageCmd(imageName).exec();
            return response.getId();
        } catch(NotFoundException e) {
            try {
                System.out.println("Pulling image '" + imageName + "'");
                PullImageCmd cmd = dockerClient.pullImageCmd(imageName);
                PullImageResultCallback callback = new PullImageResultCallback();
                cmd.exec(callback);
                if (!callback.awaitCompletion(5, TimeUnit.MINUTES)) {
                    throw new DockerOperatorException("Timed out pulling '" + imageName + "' image");
                }

                InspectImageResponse response = dockerClient.inspectImageCmd(imageName).exec();
                return response.getId();
            } catch(Exception e1) {
                throw new DockerOperatorException("Problem pulling '" + imageName + "' image", e1);
            }
        } catch(Exception e) {
            throw new DockerOperatorException("Problem inspecting '" + imageName + "' image", e);
        }

    }


}
