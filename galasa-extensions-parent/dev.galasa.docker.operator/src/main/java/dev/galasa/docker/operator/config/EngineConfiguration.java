/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.config;

public class EngineConfiguration {

    private String controllerImage; 
    private String controllerVersion;

    private String engineImage;
    private String engineVersion;
    private int    maxEngines;
    
    public void normalise() {
        if (this.controllerImage == null || this.controllerImage.isEmpty()) {
            this.controllerImage = "galasa-boot-embedded-amd64";
        }
        
        if (this.engineImage == null || this.engineImage.isEmpty()) {
            this.engineImage = this.controllerImage;
        }
        
        if (this.engineVersion == null || this.engineVersion.isEmpty()) {
            this.engineVersion = this.controllerVersion;
        }
        
        if (this.maxEngines <= 0) {
            this.maxEngines = 2;
        }
    }

    public String getControllerImage() {
        return controllerImage;
    }

    public void setControllerImage(String controllerImage) {
        this.controllerImage = controllerImage;
    }

    public String getControllerVersion() {
        return controllerVersion;
    }

    public void setControllerVersion(String controllerVersion) {
        this.controllerVersion = controllerVersion;
    }

    public String getEngineImage() {
        return engineImage;
    }

    public void setEngineImage(String engineImage) {
        this.engineImage = engineImage;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    public int getMaxEngines() {
        return maxEngines;
    }

    public void setMaxEngines(int maxEngines) {
        this.maxEngines = maxEngines;
    }

    
}
