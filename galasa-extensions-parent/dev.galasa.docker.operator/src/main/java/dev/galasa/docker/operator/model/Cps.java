/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import dev.galasa.docker.operator.config.CpsConfiguration;
import dev.galasa.docker.operator.config.EcosystemConfiguration;

public class Cps extends AbstractContainerResource {

    private static final String RESOURCE_NAME = "galasa_cps";

    private final CpsVolume cpsVolume;

    public Cps(Ecosystem ecosystem) {
        super(ecosystem, RESOURCE_NAME);

        this.cpsVolume = new CpsVolume(ecosystem);
        this.cpsVolume.addDependency(this);
        ecosystem.addResource(this.cpsVolume);
    }

    @Override
    public void checkResourceDefined() throws DockerOperatorException {
        this.cpsVolume.checkResourceDefined();

        String cpsVolumeName = this.cpsVolume.getName();
        String targetImageName = getTargetImageName();

        DockerClient dockerClient = getEcosystem().getDockerClient();

        String imageId = null;
        try {
            imageId = getImageId(targetImageName);
        } catch(Exception e) {
            throw new DockerOperatorException("Problem determining CPS image id", e);
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
                if (!mounts.get(0).getName().equals(cpsVolumeName)) {
                    correct = false;
                } else {
                    Volume dir = mounts.get(0).getDestination();
                    if (!dir.getPath().equals("/var/run/etcd")) {
                        correct = false;
                    }
                }
            }
        } catch(NotFoundException e) {
            found = false;
        } catch(Exception e) {
            throw new DockerOperatorException("Problem inspecting CPS container", e);
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
            System.out.println("Defining the CPS container");
            CreateContainerCmd cmd = dockerClient.createContainerCmd(RESOURCE_NAME);
            cmd.withName(RESOURCE_NAME);
            cmd.withImage(targetImageName);

            HostConfig hostConfig = new HostConfig();
            String portNumber = Integer.toString(getEcosystem().getConfiguration().getCps().getPort());
            hostConfig.withPortBindings(new PortBinding(new Binding("0.0.0.0", portNumber), new ExposedPort(2379)));

            Mount mount = new Mount();
            mount.withType(MountType.VOLUME);
            mount.withSource(cpsVolumeName);
            mount.withTarget("/var/run/etcd");
            ArrayList<Mount> mounts = new ArrayList<>();
            mounts.add(mount);

            hostConfig.withMounts(mounts);
            cmd.withHostConfig(hostConfig);

            cmd.withCmd("/usr/local/bin/etcd",
                    "--data-dir",
                    "/var/run/etcd/default.etcd",
                    "--listen-client-urls",
                    "http://0.0.0.0:2379",
                    "--advertise-client-urls",
                    "http://0.0.0.0:2379");

            cmd.exec();
        } catch(Exception e) {
            throw new DockerOperatorException("Problem creating CPS container", e);
        }
    }


    @Override
    protected void checkResourceRunning() throws DockerOperatorException {
        checkContainerRunning();

        checkDssProperty();
        checkCredsProperty();
    }


    private void checkDssProperty() throws DockerOperatorException {
        EcosystemConfiguration ecoConfig = getEcosystem().getConfiguration();
        CpsConfiguration cpsConfig = ecoConfig.getCps();

        String dssUrl = "etcd:http://" + ecoConfig.getHostname() + ":" + cpsConfig.getPort();

        try {
            checkCpsProperty("framework.credentials.store", dssUrl);
        } catch(DockerOperatorException e) {
            throw new DockerOperatorException("Unable to check or set the framework.credentials.store property", e);
        }
    }

    private void checkCredsProperty() throws DockerOperatorException {
        EcosystemConfiguration ecoConfig = getEcosystem().getConfiguration();
        CpsConfiguration cpsConfig = ecoConfig.getCps();

        String dssUrl = "etcd:http://" + ecoConfig.getHostname() + ":" + cpsConfig.getPort();

        try {
            checkCpsProperty("framework.dynamicstatus.store", dssUrl);
        } catch(DockerOperatorException e) {
            throw new DockerOperatorException("Unable to check or set the framework.dynamicstatus.store property", e);
        }
    }
    
    public void checkCpsProperty(String key, String value) throws DockerOperatorException {
        checkCpsProperty(key, value, true);
    }


    public void checkCpsProperty(String key, String value, boolean replaceIfExists) throws DockerOperatorException {
        try {
            StringBuffer sbStdout = new StringBuffer();
            StringBuffer sbStderr = new StringBuffer();

            issueCommand(sbStdout, sbStderr, 20, "etcdctl","get",key);

            String stderr = sbStderr.toString();
            if (!stderr.isEmpty()) {
                throw new DockerOperatorException("Error messages written whilst checking CPS property:-\n" + stderr);
            }

            String stdout = sbStdout.toString();

            String actualValue = "";
            if (!stdout.isEmpty()) {
                Pattern pattern = Pattern.compile("^" + key + "\n(.*)$");
                Matcher matcher = pattern.matcher(sbStdout.toString());
                if (!matcher.find()) {
                    throw new DockerOperatorException("Unrecognised output whilst checking CPS property:-\n" + sbStdout.toString());
                }
                actualValue = matcher.group(1);
            }
            
            if (!actualValue.isEmpty() && !replaceIfExists ) {
                return;
            }

            if (actualValue.equals(value)) {
                return;
            }
            
            System.out.println("Setting CPS property '" + key + "' with '" + value + "'");

            sbStdout = new StringBuffer();
            sbStderr = new StringBuffer();

            issueCommand(sbStdout, sbStderr, 20, "etcdctl","put",key, value);

            stderr = sbStderr.toString();
            if (!stderr.isEmpty()) {
                throw new DockerOperatorException("Error messages written whilst setting CPS property:-\n" + stderr);
            }

            stdout = sbStdout.toString();
            if (!"OK\n".equals(stdout)) {
                throw new DockerOperatorException("Unrecognised output whilst setting CPS property:-\n" + sbStdout.toString());
            }
        } catch(Exception e) { 
            throw new DockerOperatorException("Problem issueing CPS container exec command to get a CPS property '" + key + "'", e);
        }
    }

    protected void checkContainerRunning() throws DockerOperatorException {
        if (isContainerRunning(false)) {
            return;
        }
        System.out.println("CPS Container is down, requires start up");

        for(AbstractResource dependency : getDependencies()) {
            dependency.dependencyChanging(DependencyEvent.STARTING, this);
        }

        try {
            System.out.println("Starting CPS container");
            startContainer();
        } catch(Exception e) {
            throw new DockerOperatorException("Problem starting CPS container", e);
        }

        try {
            checkLog("ready to serve client requests", 120);
            System.out.println("CPS container is up");
        } catch(Exception e) {
            System.out.println("Failed to detect CPS up message, deleting container to force rebuild");
            deleteContainer();
            throw new DockerOperatorException("Problem waiting for CPS container started message", e);
        }

    }



    @Override
    protected void dependencyChanging(DependencyEvent event, AbstractResource resource) throws DockerOperatorException {
        if (event != DependencyEvent.DEFINING) {
            throw new DockerOperatorException("Unexpected event '" + event + " from dependency " + resource.getClass().getName());
        }

        stopContainer();
        deleteContainer();
    }


    private String getTargetImageName() {
        CpsConfiguration cpsConfig = getEcosystem().getConfiguration().getCps();
        return cpsConfig.getImage() + ":" + cpsConfig.getVersion();
    }

    public String getLocation() {
        EcosystemConfiguration ecoConfig = getEcosystem().getConfiguration();
        CpsConfiguration cpsConfig = ecoConfig.getCps();
        return "etcd:http://" + ecoConfig.getHostname() + ":" + cpsConfig.getPort();
    }


}
