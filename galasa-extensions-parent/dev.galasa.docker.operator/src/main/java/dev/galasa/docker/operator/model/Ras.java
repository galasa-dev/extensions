/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.model;

import java.util.ArrayList;
import java.util.List;

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
import com.github.dockerjava.api.model.Volume;

import dev.galasa.docker.operator.DockerOperatorException;
import dev.galasa.docker.operator.config.EcosystemConfiguration;
import dev.galasa.docker.operator.config.RasConfiguration;

public class Ras extends AbstractContainerResource {

    private static final String RESOURCE_NAME = "galasa_ras";

    private final RasVolume rasVolume;
    private final Cps       cps;

    public Ras(Ecosystem ecosystem) {
        super(ecosystem, RESOURCE_NAME);

        this.rasVolume = new RasVolume(ecosystem);
        this.rasVolume.addDependency(this);
        ecosystem.addResource(this.rasVolume);
        
        this.cps = ecosystem.getResource(Cps.class);
        this.cps.addDependency(this);
    }

    @Override
    public void checkResourceDefined() throws DockerOperatorException {
        this.rasVolume.checkResourceDefined();

        String rasVolumeName = this.rasVolume.getName();
        String targetImageName = getTargetImageName();

        DockerClient dockerClient = getEcosystem().getDockerClient();

        String imageId = null;
        try {
            imageId = getImageId(targetImageName);
        } catch(Exception e) {
            throw new DockerOperatorException("Problem determining RAS image id", e);
        }

        boolean found   = true;
        boolean correct = true;
        try {
            InspectContainerResponse response = dockerClient.inspectContainerCmd(RESOURCE_NAME).exec();

            String actualImage = response.getImageId();
            if (!imageId.equals(actualImage)) {
                correct = false;
            }

            List<com.github.dockerjava.api.command.InspectContainerResponse.Mount> mounts = response.getMounts();
            if (mounts == null || mounts.size() != 1) {
                correct = false;
            } else {
                if (!mounts.get(0).getName().equals(rasVolumeName)) {
                    correct = false;
                } else {
                    Volume dir = mounts.get(0).getDestination();
                    if (!dir.getPath().equals("/opt/couchdb/data")) {
                        correct = false;
                    }
                }
            }
        } catch(NotFoundException e) {
            found = false;
        } catch(Exception e) {
            throw new DockerOperatorException("Problem inspecting RAS container", e);
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
            System.out.println("Defining the RAS container");
            CreateContainerCmd cmd = dockerClient.createContainerCmd(RESOURCE_NAME);
            cmd.withName(RESOURCE_NAME);
            cmd.withImage(targetImageName);

            HostConfig hostConfig = new HostConfig();
            String portNumber = Integer.toString(getEcosystem().getConfiguration().getRas().getPort());
            hostConfig.withPortBindings(new PortBinding(new Binding("0.0.0.0", portNumber), new ExposedPort(5984)));

            Mount mount = new Mount();
            mount.withType(MountType.VOLUME);
            mount.withSource(rasVolumeName);
            mount.withTarget("/opt/couchdb/data");
            ArrayList<Mount> mounts = new ArrayList<>();
            mounts.add(mount);

            hostConfig.withMounts(mounts);
            cmd.withHostConfig(hostConfig);

            cmd.exec();
        } catch(Exception e) {
            throw new DockerOperatorException("Problem creating RAS container", e);
        }
    }


    @Override
    protected void checkResourceRunning() throws DockerOperatorException {
        checkContainerRunning();

        checkRasProperty();
    }


    private void checkRasProperty() throws DockerOperatorException {
        EcosystemConfiguration ecoConfig = getEcosystem().getConfiguration();
        RasConfiguration rasConfig = ecoConfig.getRas();

        String dssUrl = "couchdb:http://" + ecoConfig.getHostname() + ":" + rasConfig.getPort();

        try {
            this.cps.checkCpsProperty("framework.resultarchive.store", dssUrl);
        } catch(DockerOperatorException e) {
            throw new DockerOperatorException("Unable to check or set the framework.resultarchive.store property", e);
        }
    }

    protected void checkContainerRunning() throws DockerOperatorException {
        if (isContainerRunning(false)) {
            return;
        }
        System.out.println("RAS Container is down, requires start up");

        for(AbstractResource dependency : getDependencies()) {
            dependency.dependencyChanging(DependencyEvent.STARTING, this);
        }

        try {
            System.out.println("Starting RAS container");
            startContainer();
        } catch(Exception e) {
            throw new DockerOperatorException("Problem starting RAS container", e);
        }

        try {
            checkLog("Apache CouchDB has started on http://any:5986/", 120);
            System.out.println("RAS container is up");
        } catch(Exception e) {
            System.out.println("Failed to detect RAS up message, deleting container to force rebuild");
            deleteContainer();
            throw new DockerOperatorException("Problem waiting for RAS container started message", e);
        }

    }



    @Override
    protected void dependencyChanging(DependencyEvent event, AbstractResource resource) throws DockerOperatorException {
        switch(event) {
            case DEFINING:
            case STARTING:
            case STOPPING:
                if (resource == this.rasVolume) {
                    stopContainer();
                    deleteContainer();
                    return;
                }
                if (resource == this.cps) {
                    if (this.isContainerRunning(false)) {
                        System.out.println("CPS container status is changing, stopping RAS");
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
        RasConfiguration rasConfig = getEcosystem().getConfiguration().getRas();
        return rasConfig.getImage() + ":" + rasConfig.getVersion();
    }


}
