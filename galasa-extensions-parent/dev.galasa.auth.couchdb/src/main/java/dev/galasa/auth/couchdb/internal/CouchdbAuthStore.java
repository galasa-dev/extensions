/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb.internal;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import dev.galasa.extensions.common.api.HttpClientFactory;
import dev.galasa.extensions.common.api.LogFactory;
import dev.galasa.extensions.common.couchdb.CouchdbException;
import dev.galasa.extensions.common.couchdb.CouchdbStore;
import dev.galasa.extensions.common.couchdb.CouchdbValidator;
import dev.galasa.extensions.common.couchdb.pojos.ViewRow;
import dev.galasa.extensions.common.api.HttpRequestFactory;
import dev.galasa.framework.spi.auth.IAuthToken;
import dev.galasa.framework.spi.auth.IAuthStore;
import dev.galasa.framework.spi.auth.User;
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
public class CouchdbAuthStore extends CouchdbStore implements IAuthStore {

    public static final String TOKENS_DATABASE_NAME = "galasa_tokens";
    public static final String COUCHDB_AUTH_ENV_VAR = "GALASA_RAS_TOKEN";
    public static final String COUCHDB_AUTH_TYPE    = "Basic";

    private Log logger;

    public CouchdbAuthStore(URI authStoreUri, HttpClientFactory httpClientFactory, HttpRequestFactory requestFactory,
            LogFactory logFactory, CouchdbValidator validator) throws CouchdbException {
        super(authStoreUri, requestFactory, httpClientFactory);
        this.logger = logFactory.getLog(getClass());

        try {
            validator.checkCouchdbDatabaseIsValid(this.storeUri, this.httpClient, this.httpRequestFactory);
        } catch (CouchdbException e) {
            // TODO-EM: Add a custom error message to this exception
            throw new CouchdbException(e);
        }

    }

    @Override
    public List<IAuthToken> getTokens() throws AuthStoreException {
        logger.info("Retrieving tokens from CouchDB");
        // Get all of the documents in the tokens database
        List<ViewRow> tokenDocuments = new ArrayList<>();
        List<IAuthToken> tokens = new ArrayList<>();
        try {
            tokenDocuments = getAllDocsFromDatabase(TOKENS_DATABASE_NAME);

            // Build up a list of all the tokens using the document IDs
            for (ViewRow row : tokenDocuments) {
                tokens.add(getAuthTokenFromDocument(row.key));
            }

            logger.info("Tokens retrieved from CouchDB OK");
        } catch (CouchdbException e) {
            throw new CouchdbAuthStoreException(e);
        }
        return tokens;
    }

    /**
     * Gets an auth token from a CouchDB document with the given document ID.
     * The document is assumed to be within the tokens database in the CouchDB server.
     *
     * @param documentId the ID of the document containing the details of an auth token
     * @return the auth token stored within the given document
     * @throws AuthStoreException if there was a problem accessing the auth store or its response
     */
    private IAuthToken getAuthTokenFromDocument(String documentId) throws CouchdbException {
        return getDocumentFromDatabase(TOKENS_DATABASE_NAME, documentId, CouchdbAuthToken.class);
    }

    @Override
    public void shutdown() throws AuthStoreException {
        try {
            httpClient.close();
        } catch (IOException e) {
            // TODO-EM: Add a custom error message to this exception
            throw new AuthStoreException(e);
        }
    }

    @Override
    public void storeToken(String clientId, String description, User owner) throws AuthStoreException {
        // Create the JSON payload representing the token to store
        // TODO-EM: Add a TimeService to the constructor so that we can mock out the
        // Instant.now() call
        String tokenJson = gson.toJson(new CouchdbAuthToken(clientId, description, Instant.now(), owner));

        try {
            createDocument(TOKENS_DATABASE_NAME, tokenJson);
        } catch (CouchdbException e) {
            throw new CouchdbAuthStoreException(e);
        }
    }
}
