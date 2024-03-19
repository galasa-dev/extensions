/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal.dependencies.impl;

public enum CouchdbHttpMethods {

    GET("GET"),
    HEAD("HEAD"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    ;

    private String method;

    private CouchdbHttpMethods(String method){
        this.method = method;
    }

    public String getValue(){
        return method;
    }
}
