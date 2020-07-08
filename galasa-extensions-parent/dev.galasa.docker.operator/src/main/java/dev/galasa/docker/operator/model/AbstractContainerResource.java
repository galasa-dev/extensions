/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import dev.galasa.docker.operator.DockerOperatorException;

public abstract class AbstractContainerResource extends AbstractResource {

    private final String containerName;

    public AbstractContainerResource(Ecosystem ecosystem, String containerName) {
        super(ecosystem);
        this.containerName = containerName;
    }






    protected void deleteContainer() throws DockerOperatorException {
        DockerClient dockerClient = getEcosystem().getDockerClient();

        for(AbstractResource dependency : getDependencies()) {
            dependency.dependencyChanging(DependencyEvent.STOPPING, this);
        }

        String containerId = getContainerId();
        if (containerId == null) {
            return;
        }
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch(NotFoundException e) {
            return;
        } catch(Exception e) {
            throw new DockerOperatorException("Problem deleting '" + this.containerName + "' container", e);
        }
    }

    protected void stopContainer() throws DockerOperatorException {
        DockerClient dockerClient = getEcosystem().getDockerClient();

        if (!isContainerRunning(true)) {
            return;
        }

        for(AbstractResource dependency : getDependencies()) {
            dependency.dependencyChanging(DependencyEvent.STOPPING, this);
        }

        try {
            StopContainerCmd cmd = dockerClient.stopContainerCmd(getContainerId());
            cmd.exec();
        } catch(Exception e) {
            throw new DockerOperatorException("Problem inspecting '" + this.containerName + "' container", e);
        }
    }

    protected String getContainerId() throws DockerOperatorException {
        DockerClient dockerClient = getEcosystem().getDockerClient();

        try {
            InspectContainerResponse response = dockerClient.inspectContainerCmd(this.containerName).exec();

            return response.getId();
        } catch(NotFoundException e) {
            return null;
        } catch(Exception e) {
            throw new DockerOperatorException("Problem finding container '" + this.containerName + "' id", e);
        }
    }

    protected boolean isContainerRunning(boolean includePaused) throws DockerOperatorException {
        DockerClient dockerClient = getEcosystem().getDockerClient();

        try {
            InspectContainerResponse response = dockerClient.inspectContainerCmd(this.containerName).exec();

            ContainerState state = response.getState();
            Boolean running = state.getRunning();
            Boolean restarting = state.getRestarting();
            Boolean paused = state.getPaused();
            if (running == null) {
                running = false;
            }
            if (restarting == null) {
                restarting = false;
            }
            if (paused == null) {
                paused = false;
            }

            if (running || restarting || (paused && includePaused)) {
                return true;
            } else {
                return false;
            }
        } catch(NotFoundException e) {
            return false;
        } catch(Exception e) {
            throw new DockerOperatorException("Problem inspecting '" + this.containerName + "' container", e);
        }
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

    protected void startContainer() throws DockerOperatorException {
        DockerClient dockerClient = getEcosystem().getDockerClient();
        try {
            StartContainerCmd cmd = dockerClient.startContainerCmd(getContainerId());
            cmd.exec();
        } catch(Exception e) {
            throw new DockerOperatorException("Problem starting '" + this.containerName + "' container", e);
        }

    }


    protected void checkLog(String message, long timeoutInSeconds) throws DockerOperatorException {
        DockerClient dockerClient = getEcosystem().getDockerClient();

        String containerId = getContainerId();

        try {
            Instant expire = Instant.now().plus(timeoutInSeconds, ChronoUnit.SECONDS);
            while(Instant.now().isBefore(expire)) {
                StringBuilder sb = new StringBuilder();

                LogContainerCmd cmd = dockerClient.logContainerCmd(containerId);
                cmd.withStdOut(true).withStdErr(true);
                cmd.exec(new LogContainerResultCallback() {
                    @Override
                    public void onNext(Frame item) {
                        sb.append(item.toString());
                        sb.append("\n");
                    }
                }).awaitCompletion(5, TimeUnit.SECONDS);

                String log = sb.toString();
                if (log.contains(message)) {
                    return;
                } else {
                    Thread.sleep(200);
                }
            }
            throw new DockerOperatorException("Failed to detect '" + message + "' in time in container '" + containerName + "'");
        } catch(DockerOperatorException e) {
            throw e;
        } catch(Exception e) {
            throw new DockerOperatorException("Problem detecting '" + message + "' iin container '" + containerName + "'", e);
        }

    }

    protected void issueCommand(StringBuffer stdout, StringBuffer stderr, int timeoutInSeconds, String... cmds) throws DockerOperatorException {
        DockerClient dockerClient = getEcosystem().getDockerClient();
        String containerId = getContainerId();
        try {
            ExecCreateCmd cmd = dockerClient.execCreateCmd(containerId);
            cmd.withAttachStdout(true);
            cmd.withAttachStderr(true);
            cmd.withCmd(cmds);
            cmd.withUser("root");
            ExecCreateCmdResponse response = cmd.exec();

            ByteArrayOutputStream baosStdout = new ByteArrayOutputStream();
            ByteArrayOutputStream baosStderr = new ByteArrayOutputStream();

            ExecStartResultCallback callBack = new ExecStartResultCallback(baosStdout, baosStderr);
            ExecStartCmd startCmd = dockerClient.execStartCmd(response.getId());
            startCmd.exec(callBack);
            if (!callBack.awaitCompletion(timeoutInSeconds, TimeUnit.SECONDS)) {
                throw new DockerOperatorException("Command timedout");
            }          

            stdout.append(baosStdout.toString());
            stderr.append(baosStderr.toString());
        } catch(Exception e) { 
            throw new DockerOperatorException("Problem issueing command to the container '" + containerName + "'", e);
        }

    }


    protected String getFile(String path) throws DockerOperatorException {
        DockerClient dockerClient = getEcosystem().getDockerClient();
        String containerId = getContainerId();
        InputStream is = null;
        try {
            CopyArchiveFromContainerCmd cmd = dockerClient.copyArchiveFromContainerCmd(containerId, path);
            is = cmd.exec();

            try (TarArchiveInputStream tarIs = new TarArchiveInputStream(is)) {
                TarArchiveEntry entry = tarIs.getNextTarEntry();
                while(entry != null) {
                    System.out.println(entry);

                    entry = tarIs.getNextTarEntry();
                }
            }

            return null;
        } catch(NotFoundException e) {
            return null;
        } catch(Exception e) { 
            throw new DockerOperatorException("Problem issueing command to the container '" + containerName + "'", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
                is = null;
            }
        }
    }

    protected void putFile(String path, File file) throws DockerOperatorException {
        DockerClient dockerClient = getEcosystem().getDockerClient();
        String containerId = getContainerId();
        try {
            CopyArchiveToContainerCmd cmd = dockerClient.copyArchiveToContainerCmd(containerId);
            cmd.withHostResource(file.toString());
            cmd.withRemotePath(path);
            cmd.exec();
        } catch(Exception e) { 
            throw new DockerOperatorException("Problem issueing command to the container '" + containerName + "'", e);
        } finally {
        }
    }

    private boolean checkVolumePresent(String rasVolumeName, String string) {
        // TODO Auto-generated method stub
        return false;
    }



}
