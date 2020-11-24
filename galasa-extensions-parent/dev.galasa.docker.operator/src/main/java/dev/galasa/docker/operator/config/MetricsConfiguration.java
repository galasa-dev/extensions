/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.config;

public class MetricsConfiguration {

    private String image;
    private int    port;
    
    public void normalise() {
        if (this.image == null || this.image.isEmpty()) {
            this.image = "galasa-boot-embedded-amd64";
        }
        if (this.port == 0) {
            this.port = 9010;
        }
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
