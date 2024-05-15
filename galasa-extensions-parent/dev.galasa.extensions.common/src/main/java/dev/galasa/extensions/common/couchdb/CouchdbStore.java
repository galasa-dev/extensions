/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.extensions.common.couchdb;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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
        try {
            this.storeUri = new URI(storeUri.toString().replace(URL_SCHEME + ":", ""));
        } catch (URISyntaxException e) {
            // TODO-EM: Add a custom error message to this exception
            throw new CouchdbException(e);
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
        HttpPost postTokenDoc = httpRequestFactory.getHttpPostRequest(storeUri + "/" + dbName);
        postTokenDoc.setEntity(new StringEntity(jsonContent, StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = httpClient.execute(postTokenDoc)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_CREATED) {
                throw new CouchdbException("Unable to find token - " + statusLine.toString());
            }

            // Check that the document was successfully created
            HttpEntity entity = response.getEntity();
            PutPostResponse putPostResponse = gson.fromJson(EntityUtils.toString(entity), PutPostResponse.class);
            if (!putPostResponse.ok) {
                throw new CouchdbException("Unable to create the token document - Invalid JSON response");
            }

        } catch (ParseException | IOException e) {
            throw new CouchdbException("Unable to retrieve token", e);
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
        List<ViewRow> viewRows = new ArrayList<>();
        try (CloseableHttpResponse response = httpClient.execute(getTokensDocs)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CouchdbException("Unable to find view - " + statusLine.toString());
            }

            HttpEntity entity = response.getEntity();
            String responseEntity = EntityUtils.toString(entity);
            ViewResponse tokenDocs = gson.fromJson(responseEntity, ViewResponse.class);
            viewRows = tokenDocs.rows;

            if (viewRows == null) {
                throw new CouchdbException("Unable to find rows - Invalid JSON response");
            }
        } catch (ParseException | IOException e) {
            throw new CouchdbException("Unable to retrieve view ", e);
        }
        return viewRows;
    }
}
