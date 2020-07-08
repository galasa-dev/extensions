/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.config;

public class NexusConfiguration {

    private String image;
    private String version;
    private int    port;
    
    public void normalise() {
        if (this.image == null || this.image.isEmpty()) {
            this.image = "sonatype/nexus3";
        }
        if (this.version == null || this.version.isEmpty()) {
            this.version = "3.24.0";
        }
        if (this.port == 0) {
            this.port = 8081;
        }
    }

    public String getImage() {
        return image;
    }

    public String getVersion() {
        return version;
    }

    public int getPort() {
        return this.port;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPort(int port) {
        this.port = port;
    }

    
}
