/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb.internal;

import static dev.galasa.extensions.common.Errors.*;

import java.io.IOException;
import java.net.URI;
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
import dev.galasa.framework.spi.auth.IInternalAuthToken;
import dev.galasa.framework.spi.auth.IInternalUser;
import dev.galasa.framework.spi.auth.IAuthStore;
import dev.galasa.framework.spi.utils.ITimeService;
import dev.galasa.framework.spi.auth.AuthStoreException;

/**
 * When CouchDB is being used to store user-related information, including information
 * about authentication tokens (but not the tokens themselves), this class is called
 * upon to implement the auth store.
 *
 * This class registers the auth store as the only auth store in the framework, and is
 * only used when Galasa is running in an ecosystem. It gets all of its data from a
 * CouchDB server.
 */
public class CouchdbAuthStore extends CouchdbStore implements IAuthStore {

    public static final String TOKENS_DATABASE_NAME = "galasa_tokens";
    public static final String COUCHDB_AUTH_ENV_VAR = "GALASA_AUTHSTORE_TOKEN";
    public static final String COUCHDB_AUTH_TYPE    = "Basic";

    private Log logger;
    private ITimeService timeService;

    public CouchdbAuthStore(
        URI authStoreUri,
        HttpClientFactory httpClientFactory,
        HttpRequestFactory requestFactory,
        LogFactory logFactory,
        CouchdbValidator validator,
        ITimeService timeService
    ) throws CouchdbException {
        super(authStoreUri, requestFactory, httpClientFactory);
        this.logger = logFactory.getLog(getClass());
        this.timeService = timeService;

        validator.checkCouchdbDatabaseIsValid(this.storeUri, this.httpClient, this.httpRequestFactory, timeService);
    }

    @Override
    public List<IInternalAuthToken> getTokens() throws AuthStoreException {
        logger.info("Retrieving tokens from CouchDB");
        List<ViewRow> tokenDocuments = new ArrayList<>();
        List<IInternalAuthToken> tokens = new ArrayList<>();

        try {
            // Get all of the documents in the tokens database
            tokenDocuments = getAllDocsFromDatabase(TOKENS_DATABASE_NAME);

            // Build up a list of all the tokens using the document IDs
            for (ViewRow row : tokenDocuments) {
                tokens.add(getAuthTokenFromDocument(row.key));
            }

            logger.info("Tokens retrieved from CouchDB OK");
        } catch (CouchdbException e) {
            String errorMessage = ERROR_FAILED_TO_RETRIEVE_TOKENS.getMessage(e.getMessage());
            throw new AuthStoreException(errorMessage, e);
        }
        return tokens;
    }

	public List<IInternalAuthToken> getTokensByLoginId(String loginId) throws AuthStoreException {
		logger.info("Retrieving tokens from CouchDB");
        List<ViewRow> tokenDocuments = new ArrayList<>();
        List<IInternalAuthToken> tokens = new ArrayList<>();

        try {
            // Get all of the documents in the tokens database
            tokenDocuments = getAllDocsByLoginId(TOKENS_DATABASE_NAME, loginId);

            // Build up a list of all the tokens using the document IDs
            for (ViewRow row : tokenDocuments) {
                tokens.add(getAuthTokenFromDocument(row.id));
            }

            logger.info("Tokens retrieved from CouchDB OK");
        } catch (CouchdbException e) {
            String errorMessage = ERROR_FAILED_TO_RETRIEVE_TOKENS.getMessage(e.getMessage());
            throw new AuthStoreException(errorMessage, e);
        }
        return tokens;
	}

    @Override
    public void shutdown() throws AuthStoreException {
        try {
            httpClient.close();
        } catch (IOException e) {
            String errorMessage = ERROR_GALASA_AUTH_STORE_SHUTDOWN_FAILED.getMessage(e.getMessage());
            throw new AuthStoreException(errorMessage, e);
        }
    }

    @Override
    public void storeToken(String clientId, String description, IInternalUser owner) throws AuthStoreException {
        // Create the JSON payload representing the token to store
        CouchdbUser couchdbUser = new CouchdbUser(owner);
        String tokenJson = gson.toJson(new CouchdbAuthToken(clientId, description, timeService.now(), couchdbUser));

        try {
            createDocument(TOKENS_DATABASE_NAME, tokenJson);
        } catch (CouchdbException e) {
            String errorMessage = ERROR_FAILED_TO_CREATE_TOKEN_DOCUMENT.getMessage(e.getMessage());
            throw new AuthStoreException(errorMessage, e);
        }
    }

    @Override
    public void deleteToken(String tokenId) throws AuthStoreException {
        try {
            deleteDocumentFromDatabase(TOKENS_DATABASE_NAME, tokenId);
        } catch (CouchdbException e) {
            String errorMessage = ERROR_FAILED_TO_DELETE_TOKEN_DOCUMENT.getMessage(e.getMessage());
            throw new AuthStoreException(errorMessage, e);
        }
    }

    /**
     * Gets an auth token from a CouchDB document with the given document ID.
     * The document is assumed to be within the tokens database in the CouchDB server.
     *
     * @param documentId the ID of the document containing the details of an auth token
     * @return the auth token stored within the given document
     * @throws AuthStoreException if there was a problem accessing the auth store or its response
     */
    private IInternalAuthToken getAuthTokenFromDocument(String documentId) throws CouchdbException {
        return getDocumentFromDatabase(TOKENS_DATABASE_NAME, documentId, CouchdbAuthToken.class);
    }
}
