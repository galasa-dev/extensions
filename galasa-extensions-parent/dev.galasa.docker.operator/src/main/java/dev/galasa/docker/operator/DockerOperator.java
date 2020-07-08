/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator;

import java.io.FileInputStream;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import dev.galasa.docker.operator.config.EcosystemConfiguration;
import dev.galasa.docker.operator.model.Ecosystem;

public class DockerOperator {

    private URI          dockerHost;
    private DockerClient dockerClient;

    private EcosystemConfiguration configuration;

    private Ecosystem ecosystem;
    
    private boolean shutdown         = false;
    private boolean shutdownComplete = false;


    private void run(String[] args) throws DockerOperatorException {
        boolean poll = false;
        if (args.length > 0) {
            if ("poll".equals(args[0])) {
                poll = true;
            }
        }

        // *** Add shutdown hook to allow for orderly shutdown
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        configureDockerClient();

        checkDockerEngineUp();

        loadConfiguration();

        this.ecosystem = new Ecosystem(this.configuration, this.dockerClient, this.dockerHost);

        this.ecosystem.checkResourceDefined();

        this.ecosystem.checkResourcesRunning();

        System.out.println("Initial start of the Ecosystem is complete");

        if (!poll) {
            shutdownComplete = true;
            return;
        }

        System.out.println("Starting polling loop of the Docker engine");
        Instant nextCheck = Instant.now().plus(1, ChronoUnit.MINUTES);
        while(!this.shutdown) {
            if (Instant.now().isAfter(nextCheck)) {
                System.out.println("Polling Docker engine");
                this.ecosystem.checkResourceDefined();

                this.ecosystem.checkResourcesRunning();
                nextCheck = Instant.now().plus(1, ChronoUnit.MINUTES);
            }
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DockerOperatorException("Pool sleep interrupted");
            }
        }
        this.shutdownComplete = true;
        System.out.println("Docker operator finished");
    }

    private void loadConfiguration() throws DockerOperatorException {

        String configFileLocation = "/galasaoperator.yaml";

        String env = System.getenv("CONFIG");
        if (env != null && !env.isEmpty()) {
            configFileLocation = env;
        }

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yaml = new Yaml(options);

        try {
            FileInputStream fis = new FileInputStream(configFileLocation);

            this.configuration = yaml.loadAs(fis, EcosystemConfiguration.class);


            this.configuration.normalise();

            String x = yaml.dump(this.configuration);

            System.out.println("Using the following configuration:-");
            System.out.println(x);
        } catch(Exception e) {
            throw new DockerOperatorException("Unable to load configuration", e);
        }
    }

    private void checkDockerEngineUp() throws DockerOperatorException {
        PingCmd pingCmd = this.dockerClient.pingCmd();
        try {
            pingCmd.exec(); 
        } catch(Exception e) {
            throw new DockerOperatorException("Unable to contact the Docker Engine", e);
        }
    }




    private void configureDockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .build();

        this.dockerHost = config.getDockerHost();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }




    public static void main(String[] args) throws DockerOperatorException {
        DockerOperator operator = new DockerOperator();
        try {
            operator.run(args);
        } finally {
            operator.shutdownComplete = true;
        }
    }
    
    private class ShutdownHook extends Thread {
        @Override
        public void run() {
            System.out.println("Shutdown request received");
            DockerOperator.this.shutdown = true;

            while (!DockerOperator.this.shutdownComplete) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    System.out.println("Shutdown wait was interrupted");
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }


}
