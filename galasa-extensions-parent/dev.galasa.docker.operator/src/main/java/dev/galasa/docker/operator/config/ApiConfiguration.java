/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.config;

public class ApiConfiguration {

    private String image;
    private int    port;
    private String extraBundles;
    
    public void normalise() {
        if (this.image == null || this.image.isEmpty()) {
            this.image = "galasa-boot-embedded-amd64";
        }
        if (this.port == 0) {
            this.port = 8080;
        }
        if (this.extraBundles == null || this.extraBundles.isEmpty()) {
            this.extraBundles = "dev.galasa.cps.etcd,dev.galasa.ras.couchdb";
        }
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getExtraBundles() {
        return extraBundles;
    }

    public void setExtraBundles(String extraBundles) {
        this.extraBundles = extraBundles;
    }


}
