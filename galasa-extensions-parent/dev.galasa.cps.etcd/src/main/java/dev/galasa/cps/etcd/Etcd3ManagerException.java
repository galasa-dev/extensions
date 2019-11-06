/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.cps.etcd;
//test PR
public class Etcd3ManagerException extends Exception {
    private static final long serialVersionUID = 1L;

    public Etcd3ManagerException() {
    }

    public Etcd3ManagerException(String message) {
        super(message);
    }

    public Etcd3ManagerException(Throwable cause) {
        super(cause);
    }

    public Etcd3ManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public Etcd3ManagerException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
