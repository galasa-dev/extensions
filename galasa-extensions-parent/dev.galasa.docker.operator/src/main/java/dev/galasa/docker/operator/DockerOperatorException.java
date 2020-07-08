/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.docker.operator;

public class DockerOperatorException extends Exception {
    private static final long serialVersionUID = 1L;

    public DockerOperatorException() {
    }

    public DockerOperatorException(String message) {
        super(message);
    }

    public DockerOperatorException(Throwable cause) {
        super(cause);
    }

    public DockerOperatorException(String message, Throwable cause) {
        super(message, cause);
    }

    public DockerOperatorException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
