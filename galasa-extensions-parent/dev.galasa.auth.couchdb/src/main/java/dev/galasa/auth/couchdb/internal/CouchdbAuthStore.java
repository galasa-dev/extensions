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

import org.apache.commons.logging.Log;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import dev.galasa.auth.couchdb.internal.beans.AuthDBNameViewDesign;
import dev.galasa.auth.couchdb.internal.beans.FrontEndClient;
import dev.galasa.auth.couchdb.internal.beans.UserDoc;
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
import dev.galasa.framework.spi.auth.IUser;
import dev.galasa.framework.spi.auth.IAuthStore;
import dev.galasa.framework.spi.auth.IFrontEndClient;
import dev.galasa.framework.spi.utils.ITimeService;
import dev.galasa.framework.spi.auth.AuthStoreException;

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
    public static final String USERS_DB_VIEW_NAME = "loginId-view";

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
    public List<IUser> getAllUsers() throws AuthStoreException {
        logger.info("Retrieving all users from couchdb");

        List<ViewRow> userDocuments = new ArrayList<>();
        List<IUser> users = new ArrayList<>();

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

        FrontEndClient client = new FrontEndClient();

        client.setClientName(clientName);
        client.setLastLogin(Instant.now());

        String userJson = gson.toJson(new UserDoc(loginId, List.of(client)));

        try {
            createDocument(USERS_DATABASE_NAME, userJson);
        } catch (CouchdbException e) {
            String errorMessage = ERROR_FAILED_TO_CREATE_USER_DOCUMENT.getMessage(e.getMessage());
            throw new AuthStoreException(errorMessage, e);
        }

        return;
    }

    @Override
    public IUser getUserByLoginId(String loginId) throws AuthStoreException {
        logger.info("Retrieving user by loginId from CouchDB");
        List<ViewRow> userDocument;
        IUser user = null;

        try {
            // Fetch documents matching the loginId
            userDocument = getAllDocsByLoginId(USERS_DATABASE_NAME, loginId, USERS_DB_VIEW_NAME);

            // Since loginIds are unique, there should be only one document.
            if (!userDocument.isEmpty()) {
                ViewRow row = userDocument.get(0); // Get the first entry since loginId is unique

                // Fetch the user document from the CouchDB using the ID from the row
                UserDoc fetchedUser = getUserFromDocument(row.id);
                logger.info("Fetched user: " + fetchedUser);

                if (row.value != null) {
                    AuthDBNameViewDesign nameViewDesign = gson.fromJson(gson.toJson(row.value),
                            AuthDBNameViewDesign.class);
                    fetchedUser.setVersion(nameViewDesign._rev); // Set the version from the CouchDB rev
                }

                // Assign fetchedUser to the user variable
                user = fetchedUser;
            }

            logger.info("User retrieved from CouchDB OK");

        } catch (CouchdbException e) {
            String errorMessage = ERROR_FAILED_TO_RETRIEVE_USERS.getMessage(e.getMessage());
            throw new AuthStoreException(errorMessage, e);
        }

        return user;
    }

    @Override
    public IUser updateUser(IUser user) throws AuthStoreException {
        // Take a clone of the user we are passed, so we can guarantee we are using our bean which
        // serialises to the correct format.
        UserDoc userDoc = new UserDoc(user);
        updateUserDoc(httpClient, storeUri, 0, userDoc);
        return userDoc;
    }

    private void updateUserDoc(CloseableHttpClient httpClient, URI couchdbUri, int attempts, UserDoc user)
            throws AuthStoreException {

        validateUserDoc(user);

        HttpEntityEnclosingRequestBase request = buildUpdateUserDocRequest(user, couchdbUri);

        PutPostResponse putResponse = sendPutRequestToCouchDb(request);

        validateCouchdbResponseJson(user.getUserNumber(), putResponse);

        // The version of the document in couchdb has updated, so lets update our version in the doc we were sent,
        // so that the same document could be used for another update.
        user.setVersion(putResponse.rev);
    }

    private PutPostResponse sendPutRequestToCouchDb(HttpEntityEnclosingRequestBase request) throws AuthStoreException {
        PutPostResponse putResponse;
        try {
            String entity = sendHttpRequest(request, HttpStatus.SC_CREATED);
            putResponse = gson.fromJson(entity, PutPostResponse.class);

        } catch (CouchdbException e) {
            String errorMessage = ERROR_FAILED_TO_UPDATE_USER_DOCUMENT.getMessage(e.getMessage());
            throw new AuthStoreException(errorMessage,e);
        }
        return putResponse;
    }

    private HttpEntityEnclosingRequestBase buildUpdateUserDocRequest(UserDoc user, URI couchdbUri) {
         HttpEntityEnclosingRequestBase request;

        String jsonStructure = gson.toJson(user);
        request = httpRequestFactory
                .getHttpPutRequest(couchdbUri + "/" + USERS_DATABASE_NAME + "/" + user.getUserNumber());
        request.setHeader("If-Match", user.getVersion());
    
        request.setEntity(new StringEntity(jsonStructure, StandardCharsets.UTF_8));
        return request;
    }

    private void validateCouchdbResponseJson(String expectedUserNumber , PutPostResponse putResponse) throws AuthStoreException {
        if (putResponse.id == null || putResponse.rev == null) {
            String errorMessage = ERROR_FAILED_TO_UPDATE_USER_DOCUMENT_INVALID_RESP.getMessage();
            throw new AuthStoreException(errorMessage);
        }
        if (!expectedUserNumber.equals(putResponse.id)) {
            String errorMessage = ERROR_FAILED_TO_UPDATE_USER_DOCUMENT_MISMATCH_DOC_ID.getMessage();
            throw new AuthStoreException(errorMessage);
        }
    }

    private void validateUserDoc(UserDoc user) throws AuthStoreException {
        if (user.getUserNumber() == null) {
            String errorMessage = ERROR_FAILED_TO_UPDATE_USER_DOCUMENT_INPUT_INVALID_NULL_USER_NUMBER.getMessage();
            throw new AuthStoreException(errorMessage);
        } 

        if (user.getVersion() == null) {
            String errorMessage = ERROR_FAILED_TO_UPDATE_USER_DOCUMENT_INPUT_INVALID_NULL_USER_VERSION.getMessage();
            throw new AuthStoreException(errorMessage);
        } 
    }

    @Override
    public void deleteUser(IUser user) throws AuthStoreException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteUser'");
    }

    @Override
    public IFrontEndClient createClient(String clientName) {
        return new FrontEndClient(clientName, timeService.now());
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
