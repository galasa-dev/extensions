/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb.internal;

import static dev.galasa.extensions.common.Errors.*;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import dev.galasa.auth.couchdb.internal.beans.AuthDBNameViewDesign;
import dev.galasa.extensions.common.api.HttpClientFactory;
import dev.galasa.extensions.common.api.LogFactory;
import dev.galasa.extensions.common.couchdb.CouchdbException;
import dev.galasa.extensions.common.couchdb.CouchdbStore;
import dev.galasa.extensions.common.couchdb.CouchdbValidator;
import dev.galasa.extensions.common.couchdb.pojos.PutPostResponse;
import dev.galasa.extensions.common.couchdb.pojos.ViewRow;
import dev.galasa.extensions.common.api.HttpRequestFactory;
import dev.galasa.framework.spi.auth.IInternalAuthToken;
import dev.galasa.framework.spi.auth.IInternalUser;
import dev.galasa.framework.spi.auth.UserDoc;
import dev.galasa.framework.spi.auth.IAuthStore;
import dev.galasa.framework.spi.utils.ITimeService;
import dev.galasa.framework.spi.auth.AuthStoreException;
import dev.galasa.framework.spi.auth.FrontendClient;

/**
 * When CouchDB is being used to store user-related information, including
 * information
 * about authentication tokens (but not the tokens themselves), this class is
 * called
 * upon to implement the auth store.
 *
 * This class registers the auth store as the only auth store in the framework,
 * and is
 * only used when Galasa is running in an ecosystem. It gets all of its data
 * from a
 * CouchDB server.
 */
public class CouchdbAuthStore extends CouchdbStore implements IAuthStore {

    public static final String TOKENS_DATABASE_NAME = "galasa_tokens";
    public static final String USERS_DATABASE_NAME = "galasa_users";
    public static final String COUCHDB_AUTH_ENV_VAR = "GALASA_AUTHSTORE_TOKEN";
    public static final String COUCHDB_AUTH_TYPE = "Basic";

    public static final String TOKENS_DB_VIEW_NAME = "loginId-view";
    public static final String USERS_DB_VIEW_NAME = "users-loginId-view";

    private Log logger;
    private ITimeService timeService;

    public CouchdbAuthStore(
            URI authStoreUri,
            HttpClientFactory httpClientFactory,
            HttpRequestFactory requestFactory,
            LogFactory logFactory,
            CouchdbValidator validator,
            ITimeService timeService) throws CouchdbException {
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
            tokenDocuments = getAllDocsByLoginId(TOKENS_DATABASE_NAME, loginId, TOKENS_DB_VIEW_NAME);

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
     * The document is assumed to be within the tokens database in the CouchDB
     * server.
     *
     * @param documentId the ID of the document containing the details of an auth
     *                   token
     * @return the auth token stored within the given document
     * @throws AuthStoreException if there was a problem accessing the auth store or
     *                            its response
     */
    private IInternalAuthToken getAuthTokenFromDocument(String documentId) throws CouchdbException {
        return getDocumentFromDatabase(TOKENS_DATABASE_NAME, documentId, CouchdbAuthToken.class);
    }

    @Override
    public List<UserDoc> getAllUsers() throws AuthStoreException {
        logger.info("Retrieving all users from couchdb");

        List<ViewRow> userDocuments = new ArrayList<>();
        List<UserDoc> users = new ArrayList<>();

        try {
            userDocuments = getAllDocsFromDatabase(USERS_DATABASE_NAME);

            for (ViewRow row : userDocuments) {
                users.add(getUserFromDocument(row.id));
            }

            logger.info("Users retrieved from CouchDB OK");

        } catch (CouchdbException e) {
            String errorMessage = ERROR_FAILED_TO_RETRIEVE_USERS.getMessage(e.getMessage());
            throw new AuthStoreException(errorMessage, e);
        }

        return users;
    }

    @Override
    public void createUser(String loginId, String clientName) throws AuthStoreException {
        String userJson = gson.toJson(new UserDoc(loginId, List.of(new FrontendClient(clientName, Instant.now()))));

        try {
            createDocument(USERS_DATABASE_NAME, userJson);
        } catch (CouchdbException e) {
            String errorMessage = ERROR_FAILED_TO_CREATE_USER_DOCUMENT.getMessage(e.getMessage());
            throw new AuthStoreException(errorMessage, e);
        }
    }

    @Override
    public UserDoc getUserByLoginId(String loginId) throws AuthStoreException {
        logger.info("Retrieving user by loginId from CouchDB");
        List<ViewRow> userDocument = new ArrayList<>();
        List<UserDoc> users = new ArrayList<>();

        UserDoc user = null;
        String revNumber = null;

        try {

            userDocument = getAllDocsByLoginId(USERS_DATABASE_NAME, loginId, USERS_DB_VIEW_NAME);

            // Build up a list of all the tokens using the document IDs
            for (ViewRow row : userDocument) {
                users.add(getUserFromDocument(row.id));

                if (row.value != null) {
                    AuthDBNameViewDesign nameViewDesign = gson.fromJson(gson.toJson(row.value),
                    AuthDBNameViewDesign.class);
                    revNumber = nameViewDesign._rev;
                }

            }
            logger.info("User retrieved from CouchDB OK");

        } catch (CouchdbException e) {
            String errorMessage = ERROR_FAILED_TO_RETRIEVE_USERS.getMessage(e.getMessage());
            throw new AuthStoreException(errorMessage, e);
        }

        // Always going to return one entry, as loginIds are unique.
        // Hence, we can take out the first index
        if (!users.isEmpty()) {

            user = users.get(0);
            user.setUserNumber(userDocument.get(0).id);
            user.setVersion(revNumber);

        }

        return user;

    }

    @Override
    public void updateUserClientActivity(String loginId, String clientName) throws AuthStoreException {

        UserDoc user = getUserByLoginId(loginId);

        List<FrontendClient> clients = user.getClients();

        Optional<FrontendClient> clientOptional = clients.stream()
                .filter(client -> client.getClientName().equals(clientName))
                .findFirst();

        if (clientOptional.isPresent()) {
            FrontendClient client = clientOptional.get();
            client.setLastLoggedIn(Instant.now());
        } else {
            // User has used a new client.
            FrontendClient newClient = new FrontendClient(clientName, Instant.now());
            clients.add(newClient);
        }

        try {
            updateUserDoc(httpClient, storeUri, 0, user);
        } catch (CouchdbException e) {
            e.printStackTrace();
            throw new AuthStoreException(e);
        }
    }

    private void updateUserDoc(CloseableHttpClient httpClient, URI couchdbUri, int attempts, UserDoc user)
            throws CouchdbException {

        String jsonStructure = gson.toJson(user);

        HttpEntityEnclosingRequestBase request;

        if (user.getUserNumber() == null) {
            request = httpRequestFactory.getHttpPostRequest(couchdbUri + "/" + USERS_DATABASE_NAME);
        } else {
            request = httpRequestFactory
                    .getHttpPutRequest(couchdbUri + "/" + USERS_DATABASE_NAME + "/" + user.getUserNumber());
            request.setHeader("If-Match", user.getVersion());

            logger.info("Rev is: " + user.getVersion());
        }

        request.setEntity(new StringEntity(jsonStructure, StandardCharsets.UTF_8));

        try {
            String entity = sendHttpRequest(request, HttpStatus.SC_CREATED);
            PutPostResponse putPostResponse = gson.fromJson(entity, PutPostResponse.class);

            if (putPostResponse.id == null || putPostResponse.rev == null) {
                throw new CouchdbException("Unable to store the user structure - Invalid JSON response");
            }

            user.setUserNumber(putPostResponse.id);
            user.setVersion(putPostResponse.rev);

        } catch (CouchdbException e) {
            throw new CouchdbException(e);
        }

    }

    /**
     * Gets a user from a CouchDB document with the given document ID.
     * The document is assumed to be within the users database in the CouchDB
     * server.
     *
     * @param documentId the ID of the document containing the details of a user
     * @return the user stored within the given document
     * @throws UsersStoreException if there was a problem accessing the users store
     *                             or its response
     */
    private UserDoc getUserFromDocument(String documentId) throws CouchdbException {
        return getDocumentFromDatabase(USERS_DATABASE_NAME, documentId, UserDoc.class);
    }

}
