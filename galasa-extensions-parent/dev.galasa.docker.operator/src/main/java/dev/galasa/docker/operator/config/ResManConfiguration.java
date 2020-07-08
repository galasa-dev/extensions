/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator.config;

public class ResManConfiguration {

    private String image;
    
    public void normalise() {
        if (this.image == null || this.image.isEmpty()) {
            this.image = "galasa-boot-embedded-amd64";
        }
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }


}
