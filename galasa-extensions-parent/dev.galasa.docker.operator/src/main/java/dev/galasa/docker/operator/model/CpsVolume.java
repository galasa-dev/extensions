/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.model;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateVolumeCmd;
import com.github.dockerjava.api.exception.NotFoundException;

import dev.galasa.docker.operator.DockerOperatorException;

public class CpsVolume extends AbstractResource {

    private static final String RESOURCE_NAME = "galasa_cps_volume";

    public CpsVolume(Ecosystem ecosystem) {
        super(ecosystem);
    }

    @Override
    public void checkResourceDefined() throws DockerOperatorException {

        DockerClient dockerClient = getEcosystem().getDockerClient();

        try {
            dockerClient.inspectVolumeCmd(RESOURCE_NAME).exec();

            return;
        } catch(NotFoundException e) {
            System.out.println("CPS Volume is missing");
        } catch(Exception e) {
            throw new DockerOperatorException("Problem inspecting the CPS volume resource", e);
        }

        for(AbstractResource resource : getDependencies()) {
            resource.dependencyChanging(DependencyEvent.DEFINING, this);
        }

        try {
            CreateVolumeCmd createCommand = dockerClient.createVolumeCmd();  
            createCommand.withName(RESOURCE_NAME);
            createCommand.exec();
            System.out.println("CPS volume created");
        } catch(Exception e) {
            throw new DockerOperatorException("Problem creating the CPS volume", e);
        }
    }

    public String getName() {
        return RESOURCE_NAME;
    }

}
