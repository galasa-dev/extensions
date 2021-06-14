/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020-2021.
 */
package dev.galasa.docker.operator.config;

public class SimPlatformConfiguration {

    private String image;
    private String version;
    private int    telnetPort;
    private int    webservicePort;
    private int    databasePort;
    private int    zosmfPort;
    
    public void normalise() {
        if (this.image == null || this.image.isEmpty()) {
            this.image = "galasa-simplatform-amd64";
        }
        
        if (this.telnetPort <= 0) {
            this.telnetPort = 2023;
        }

        if (this.webservicePort <= 0) {
            this.webservicePort = 2080;
        }

        if (this.databasePort <= 0) {
            this.databasePort = 2027;
        }

        if (this.zosmfPort <= 0) {
            this.zosmfPort = 2040;
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

    public int getTelnetPort() {
        return telnetPort;
    }

    public void setTelnetPort(int telnetPort) {
        this.telnetPort = telnetPort;
    }

    public int getWebservicePort() {
        return webservicePort;
    }

    public void setWebservicePort(int webservicePort) {
        this.webservicePort = webservicePort;
    }

    public int getDatabasePort() {
        return databasePort;
    }

    public void setDatabasePort(int databasePort) {
        this.databasePort = databasePort;
    }

    public int getZosmfPort() {
        return zosmfPort;
    }

    public void setZosmfPort(int zosmfPort) {
        this.zosmfPort = zosmfPort;
    }

}
