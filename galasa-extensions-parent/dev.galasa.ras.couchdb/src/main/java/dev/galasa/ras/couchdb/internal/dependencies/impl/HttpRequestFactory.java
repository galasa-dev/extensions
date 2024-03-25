/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal.dependencies.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;

import dev.galasa.framework.spi.Environment;

public class HttpRequestFactory {

    private final String token = "GALASA_RAS_TOKEN";

    private Map<String, String> headers = new HashMap<String,String>();

    public HttpRequestFactory(Environment environment) {
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put( "Authorization", environment.getenv(token));
    }

    private  HttpRequest addDefaultHeaders(HttpRequest request) {
        for (Map.Entry<String,String> header : headers.entrySet()) {
            request.addHeader(header.getKey(), header.getValue());
        }
        return request;
    }
 
    public HttpGet getHttpGetRequest(String url) {
        HttpGet request = (HttpGet) addDefaultHeaders(new HttpGet(url));
        return request;
    }

    public HttpHead getHttpHeadRequest(String url) {
        HttpHead request = (HttpHead) addDefaultHeaders(new HttpHead(url));
        return request;
    }

    public HttpPost getHttpPostRequest(String url) {
        HttpPost request = (HttpPost) addDefaultHeaders(new HttpPost(url));
        return request;
    }

    public HttpPut getHttpPutRequest(String url) {
        HttpPut request = (HttpPut) addDefaultHeaders(new HttpPut(url));
        return request;
    }

    public HttpDelete getHttpDeleteRequest(String url) {
        HttpDelete request = (HttpDelete) addDefaultHeaders(new HttpDelete(url));
        return request;
    }

}