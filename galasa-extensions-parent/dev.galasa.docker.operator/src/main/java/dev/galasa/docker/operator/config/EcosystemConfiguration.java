/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020-2021.
 */
package dev.galasa.docker.operator.config;

import dev.galasa.docker.operator.DockerOperatorException;

public class EcosystemConfiguration {

    private String hostname;
    private String galasaRegistry;
    private String version;
    
    private CpsConfiguration cps;
    private RasConfiguration ras;
    private ApiConfiguration api;
    private ResManConfiguration      resourceManagement;
    private MetricsConfiguration     metrics;
    private EngineConfiguration      engineController;
    private SimPlatformConfiguration simplatform;
    private NexusConfiguration       nexus;

    public String getHostname() {
        return this.hostname;
    }

    public String getVersion() {
        return this.version;
    }

    public void normalise() throws DockerOperatorException {
        if (this.hostname == null || this.hostname.isEmpty()) {
            this.hostname = System.getenv("ECOSYSTEM_HOSTNAME");

            if (this.hostname == null || this.hostname.isEmpty()) {
                throw new DockerOperatorException("Ecosystem hostname is missing");
            }
        }

        if (this.version == null || this.version.isEmpty()) {
            this.version = "0.12.0";
        }
        
        if (this.galasaRegistry == null || this.galasaRegistry.isEmpty()) {
            this.galasaRegistry = "galasadev";
        }
        
        if (this.cps == null) {
            this.cps = new CpsConfiguration();
        }
        this.cps.normalise();
        
        if (this.ras == null) {
            this.ras = new RasConfiguration();
        }
        this.ras.normalise();
        
        if (this.api == null) {
            this.api = new ApiConfiguration();
        }
        this.api.normalise();
        
        if (this.resourceManagement == null) {
            this.resourceManagement = new ResManConfiguration();
        }
        this.resourceManagement.normalise();
        
        if (this.metrics == null) {
            this.metrics = new MetricsConfiguration();
        }
        this.metrics.normalise();
        
        if (this.engineController == null) {
            this.engineController = new EngineConfiguration();
        }
        this.engineController.normalise();
        
        if (this.simplatform == null) {
            this.simplatform = new SimPlatformConfiguration();
        }
        this.simplatform.normalise();
        
        if (this.nexus == null) {
            this.nexus = new NexusConfiguration();
        }
        this.nexus.normalise();
    }

    public String getGalasaRegistry() {
        return galasaRegistry;
    }

    public void setGalasaRegistry(String galasaRegistry) {
        this.galasaRegistry = galasaRegistry;
    }

    public CpsConfiguration getCps() {
        return cps;
    }

    public void setCps(CpsConfiguration cps) {
        this.cps = cps;
    }

    public RasConfiguration getRas() {
        return ras;
    }

    public void setRas(RasConfiguration ras) {
        this.ras = ras;
    }

    public ApiConfiguration getApi() {
        return api;
    }

    public void setApi(ApiConfiguration api) {
        this.api = api;
    }

    public ResManConfiguration getResourceManagement() {
        return resourceManagement;
    }

    public void setResourceManagement(ResManConfiguration resourceManagement) {
        this.resourceManagement = resourceManagement;
    }

    public MetricsConfiguration getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsConfiguration metrics) {
        this.metrics = metrics;
    }

    public EngineConfiguration getEngineController() {
        return engineController;
    }

    public void setEngineController(EngineConfiguration engineController) {
        this.engineController = engineController;
    }

    public SimPlatformConfiguration getSimplatform() {
        return simplatform;
    }

    public void setSimplatform(SimPlatformConfiguration simplatform) {
        this.simplatform = simplatform;
    }

    public NexusConfiguration getNexus() {
        return nexus;
    }

    public void setNexus(NexusConfiguration nexus) {
        this.nexus = nexus;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
    

}
