/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import dev.galasa.extensions.common.api.HttpClientFactory;
import dev.galasa.extensions.common.api.LogFactory;
import dev.galasa.extensions.common.couchdb.CouchdbAuthStoreException;
import dev.galasa.extensions.common.couchdb.CouchdbValidator;
import dev.galasa.extensions.common.couchdb.pojos.ViewResponse;
import dev.galasa.extensions.common.couchdb.pojos.ViewRow;
import dev.galasa.extensions.common.impl.HttpRequestFactory;
import dev.galasa.framework.spi.auth.AuthToken;
import dev.galasa.framework.spi.auth.IAuthStore;
import dev.galasa.framework.spi.utils.GalasaGson;
import dev.galasa.framework.spi.auth.AuthStoreException;

/**
 * When CouchDB is being used to store user-related information, including authentication
 * tokens, this class is called upon to implement the auth store. This class registers the
 * auth store as the only auth store in the framework, and is only used when Galasa is
 * running in an ecosystem.
 *
 * This implementation of the auth store interface gets all of its data from a CouchDB
 * server.
 */
public class CouchdbAuthStore implements IAuthStore {

    public static final String URL_SCHEMA = "couchdb";
    public static final String TOKENS_DATABASE_NAME = "galasa_tokens";

    private final GalasaGson gson = new GalasaGson();

    private URI authStoreUri;
    private CloseableHttpClient httpClient;
    private Log logger;
    private HttpRequestFactory httpRequestFactory;

    public CouchdbAuthStore(URI authStoreUri, HttpClientFactory httpClientFactory, HttpRequestFactory requestFactory, LogFactory logFactory, CouchdbValidator validator) throws AuthStoreException {

        // Strip off the 'couchdb:' prefix from the auth store URI
        // e.g. couchdb:https://myhost:5984 becomes https://myhost:5984
        try {
            this.authStoreUri = new URI(authStoreUri.toString().replace(URL_SCHEMA + ":", ""));
        } catch (URISyntaxException e) {
            // TODO-EM: Add a custom error message to this exception
            throw new AuthStoreException();
        }

        this.httpRequestFactory = requestFactory;
        this.logger = logFactory.getLog(getClass());
        this.httpClient = httpClientFactory.createClient();

        validator.checkCouchdbDatabaseIsValid(this.authStoreUri, this.httpClient, this.httpRequestFactory);
    }

    @Override
    public List<AuthToken> getTokens() throws AuthStoreException {
        logger.info("Retrieving tokens from CouchDB");
        // Get all of the documents in the tokens database
        List<ViewRow> tokenDocuments = getAllDocsViewRows(TOKENS_DATABASE_NAME);

        // Build up a list of all the tokens using the document IDs
        List<AuthToken> tokens = new ArrayList<>();
        for (ViewRow row : tokenDocuments) {
            tokens.add(getAuthTokenFromDocument(row.key));
        }

        logger.info("Tokens retrieved from CouchDB OK");
        return tokens;
    }

    /**
     * Sends a GET request to CouchDB's /{db}/_all_docs endpoint and returns the "rows" list in the response,
     * which corresponds to the list of documents within the given database.
     *
     * @param dbName the name of the database to retrieve the documents of
     * @return a list of rows corresponding to documents within the database
     * @throws AuthStoreException if there was a problem accessing the auth store or its response
     */
    private List<ViewRow> getAllDocsViewRows(String dbName) throws AuthStoreException {
        HttpGet getTokensDocs = httpRequestFactory.getHttpGetRequest(authStoreUri + "/" + dbName + "/_all_docs");
        List<ViewRow> viewRows = new ArrayList<>();
        try (CloseableHttpResponse response = httpClient.execute(getTokensDocs)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CouchdbAuthStoreException("Unable to find view - " + statusLine.toString());
            }

            HttpEntity entity = response.getEntity();
            String responseEntity = EntityUtils.toString(entity);
            ViewResponse tokenDocs = gson.fromJson(responseEntity, ViewResponse.class);
            viewRows = tokenDocs.rows;

            if (viewRows == null) {
                throw new CouchdbAuthStoreException("Unable to find rows - Invalid JSON response");
            }
        } catch (ParseException | IOException e) {
            throw new AuthStoreException("Unable to retrieve view ", e);
        }
        return viewRows;
    }

    /**
     * Gets an auth token from a CouchDB document with the given document ID.
     * The document is assumed to be within the tokens database in the CouchDB server.
     *
     * @param documentId the ID of the document containing the details of an auth token
     * @return the auth token stored within the given document
     * @throws AuthStoreException if there was a problem accessing the auth store or its response
     */
    private AuthToken getAuthTokenFromDocument(String documentId) throws AuthStoreException {
        HttpGet getTokenDoc = httpRequestFactory.getHttpGetRequest(authStoreUri + "/" + TOKENS_DATABASE_NAME + "/" + documentId);
        AuthToken token = null;
        try (CloseableHttpResponse response = httpClient.execute(getTokenDoc)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CouchdbAuthStoreException("Unable to find token - " + statusLine.toString());
            }

            HttpEntity entity = response.getEntity();
            String responseEntity = EntityUtils.toString(entity);
            token = gson.fromJson(responseEntity, AuthToken.class);

        } catch (ParseException | IOException e) {
            throw new AuthStoreException("Unable to retrieve token", e);
        }
        return token;
    }

    @Override
    public void shutdown() throws AuthStoreException {
        try {
            httpClient.close();
        } catch (IOException e) {
            // TODO-EM: Add a custom error message to this exception
            throw new AuthStoreException();
        }
    }
}
