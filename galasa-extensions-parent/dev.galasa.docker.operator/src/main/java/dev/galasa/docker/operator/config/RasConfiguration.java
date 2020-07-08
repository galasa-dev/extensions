/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.config;

public class RasConfiguration {

    private String image;
    private String version;
    private int    port;
    
    public void normalise() {
        if (this.image == null || this.image.isEmpty()) {
            this.image = "couchdb";
        }
        if (this.version == null || this.version.isEmpty()) {
            this.version = "2.3.1";
        }
        if (this.port == 0) {
            this.port = 5984;
        }
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
