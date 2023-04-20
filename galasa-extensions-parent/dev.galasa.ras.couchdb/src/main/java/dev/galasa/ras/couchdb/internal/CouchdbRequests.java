/*
* Copyright contributors to the Galasa project
*/
package dev.galasa.ras.couchdb.internal;


import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;

public class CouchdbRequests {
    static String authorizationString = System.getenv("GALASA_RAS_TOKEN");

    public static HttpDelete deleteRequest (String url){
        HttpDelete delete = new HttpDelete(url);
        delete.addHeader( "Authorization",CouchdbRequests.authorizationString);
        return delete;
    }

    public static HttpGet getRequest( String url){
        HttpGet get = new HttpGet(url);
        get.addHeader("Accept", "application/json");
        get.addHeader( "Authorization",CouchdbRequests.authorizationString);
        return get;
    }

    public static HttpHead headRequest( String url){
        HttpHead head = new HttpHead(url);
        head.addHeader("Accept", "application/json");
        head.addHeader("Content-Type", "application/json");
        head.addHeader( "Authorization",CouchdbRequests.authorizationString);
        return head;
    }

    public static HttpPost postRequest( String url){
        HttpPost post = new HttpPost(url);
        post.addHeader("Accept", "application/json");
        post.addHeader("Content-Type", "application/json");
        post.addHeader( "Authorization",CouchdbRequests.authorizationString);
        return post;
    }

    public static HttpPut putRequest (String url){
        HttpPut put = new HttpPut(url);
        put.addHeader("Accept", "application/json");
        put.addHeader("Content-Type", "application/json");
        put.addHeader( "Authorization",CouchdbRequests.authorizationString);
        return put;
    }
}
