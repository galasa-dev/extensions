/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.extensions.common.couchdb;

import static dev.galasa.extensions.common.Errors.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import dev.galasa.extensions.common.api.HttpClientFactory;
import dev.galasa.extensions.common.api.HttpRequestFactory;
import dev.galasa.extensions.common.couchdb.pojos.PutPostResponse;
import dev.galasa.extensions.common.couchdb.pojos.ViewResponse;
import dev.galasa.extensions.common.couchdb.pojos.ViewRow;
import dev.galasa.framework.spi.utils.GalasaGson;

/**
 * This is a base class for CouchDB-implementations of Galasa stores that defines functions for common interactions
 * with CouchDB, including creating documents in a database and getting all documents that are stored in a database.
 */
public abstract class CouchdbStore {

    public static final String URL_SCHEME = "couchdb";

    protected final URI storeUri;

    protected HttpRequestFactory httpRequestFactory;
    protected CloseableHttpClient httpClient;
    protected GalasaGson gson = new GalasaGson();

    public CouchdbStore(URI storeUri, HttpRequestFactory httpRequestFactory, HttpClientFactory httpClientFactory) throws CouchdbException {
        // Strip off the 'couchdb:' prefix from the auth store URI
        // e.g. couchdb:https://myhost:5984 becomes https://myhost:5984
        String storeUriStr = storeUri.toString();
        try {
            this.storeUri = new URI(storeUriStr.replace(URL_SCHEME + ":", ""));
        } catch (URISyntaxException e) {
            String errorMessage = ERROR_URI_IS_INVALID.getMessage(storeUriStr, e.getMessage());
            throw new CouchdbException(errorMessage, e);
        }

        this.httpRequestFactory = httpRequestFactory;
        this.httpClient = httpClientFactory.createClient();
    }

    /**
     * Creates a new document in the given database with the given JSON content.
     *
     * @param dbName the database to create the new document within
     * @param jsonContent the JSON content to send to CouchDB in order to populate the new document
     * @throws CouchdbException if there is a problem accessing the CouchDB server or creating the document
     */
    protected void createDocument(String dbName, String jsonContent) throws CouchdbException {
        // Create a new document in the tokens database with the new token to store
        HttpPost postDocument = httpRequestFactory.getHttpPostRequest(storeUri + "/" + dbName);
        postDocument.setEntity(new StringEntity(jsonContent, StandardCharsets.UTF_8));
        String responseEntity = actionHttpRequest(postDocument, HttpStatus.SC_CREATED);

        // Check that the document was successfully created
        PutPostResponse putPostResponse = gson.fromJson(responseEntity, PutPostResponse.class);
        if (!putPostResponse.ok) {
            String errorMessage = ERROR_FAILED_TO_CREATE_COUCHDB_DOCUMENT.getMessage(dbName);
            throw new CouchdbException(errorMessage);
        }
    }

    /**
     * Sends a GET request to CouchDB's /{db}/_all_docs endpoint and returns the "rows" list in the response,
     * which corresponds to the list of documents within the given database.
     *
     * @param dbName the name of the database to retrieve the documents of
     * @return a list of rows corresponding to documents within the database
     * @throws CouchdbException if there was a problem accessing the CouchDB store or its response
     */
    protected List<ViewRow> getAllDocsFromDatabase(String dbName) throws CouchdbException {
        HttpGet getTokensDocs = httpRequestFactory.getHttpGetRequest(storeUri + "/" + dbName + "/_all_docs");
        String responseEntity = actionHttpRequest(getTokensDocs, HttpStatus.SC_OK);

        ViewResponse allDocs = gson.fromJson(responseEntity, ViewResponse.class);
        List<ViewRow> viewRows = allDocs.rows;

        if (viewRows == null) {
            String errorMessage = ERROR_FAILED_TO_GET_DOCUMENTS_FROM_DATABASE.getMessage(dbName);
            throw new CouchdbException(errorMessage);
        }

        return viewRows;
    }

    /**
     * Gets an object from a given database's document using its document ID by sending a
     * GET /{db}/{docid} request to the CouchDB server.
     *
     * @param <T> The object type to be returned
     * @param dbName the name of the database to retrieve the document from
     * @param documentId the couchDB ID for the document to retrieve
     * @param classOfObject the class of the JSON object to retrieve from the CouchDB Document
     * @return an object of the class provided in classOfObject
     * @throws CouchdbException if there was a problem accessing the CouchDB store or its response
     */
    protected <T> T getDocumentFromDatabase(String dbName, String documentId, Class<T> classOfObject) throws CouchdbException {
        HttpGet getTokenDoc = httpRequestFactory.getHttpGetRequest(storeUri + "/" + dbName + "/" + documentId);
        return gson.fromJson(actionHttpRequest(getTokenDoc, HttpStatus.SC_OK), classOfObject);
    }


    /**
     * Sends a given HTTP request to the CouchDB server and returns the response body as a string.
     *
     * @param httpRequest the HTTP request to send to the CouchDB server
     * @param expectedHttpStatusCode the expected Status code to get from the CouchDb server upon the request being actioned
     * @return a string representation of the response.
     * @throws CouchdbException if there was a problem accessing the CouchDB store or its response
     */
    private String actionHttpRequest(HttpUriRequest httpRequest, int expectedHttpStatusCode) throws CouchdbException {
        String responseEntity = "";
        try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
            StatusLine statusLine = response.getStatusLine();
            int actualStatusCode = statusLine.getStatusCode();
            if (actualStatusCode != expectedHttpStatusCode) {
                String errorMessage = ERROR_UNEXPECTED_COUCHDB_HTTP_RESPONSE.getMessage(httpRequest.getURI().toString(), actualStatusCode, expectedHttpStatusCode);
                throw new CouchdbException(errorMessage);
            }

            HttpEntity entity = response.getEntity();
            responseEntity = EntityUtils.toString(entity);

        } catch (ParseException | IOException e) {
            String errorMessage = ERROR_FAILURE_OCCURRED_WHEN_CONTACTING_COUCHDB.getMessage(httpRequest.getURI().toString(), e.getMessage());
            throw new CouchdbException(errorMessage, e);
        }
        return responseEntity;
    }
}
