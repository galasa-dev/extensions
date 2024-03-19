/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal.dependencies.impl;

import java.net.URI;
import java.util.Map;

import dev.galasa.ras.couchdb.internal.CouchdbRasException;


import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;

public class CouchdbHttpRequestFactory {

    private static String authorizationString = System.getenv("GALASA_RAS_TOKEN");
    private String requestBody;
    private  HttpRequest.Builder requestBuilder;
    private Map<String, String> headers;

    public CouchdbHttpRequestFactory() {
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put( "Authorization", authorizationString);
    }

    private  HttpRequest.Builder addHeaders( HttpRequest.Builder request) {
        for (Map.Entry<String,String> header : headers.entrySet()){
            request.setHeader(header.getKey(), header.getValue());
        }
        return request;
    }
  
    public HttpRequest getHttpRequest(URI url, CouchdbHttpMethods method) throws CouchdbRasException{
       
        requestBuilder = HttpRequest.newBuilder(url);
        requestBuilder = addHeaders(requestBuilder);
        BodyPublisher publisher = BodyPublishers.ofString(requestBody);
        requestBuilder.method(method.getValue(), publisher);
        return requestBuilder.build();
    }

    public HttpRequest getHttpRequest(URI url, CouchdbHttpMethods method, String requestBody) throws CouchdbRasException{
        this.requestBody = requestBody;
        return getHttpRequest(url, method);
    }
}