/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.http.impl.client.CloseableHttpClient;

import dev.galasa.extensions.common.api.HttpClientFactory;
import dev.galasa.extensions.common.api.LogFactory;
import dev.galasa.framework.spi.auth.AuthToken;
import dev.galasa.framework.spi.auth.IUserStore;
import dev.galasa.framework.spi.auth.UserStoreException;

/**
 * When CouchDB is being used to store user-related information, including authentication
 * tokens, this class is called upon to implement the user store. This class registers the
 * user store as the only user store in the framework, and is only used when Galasa is
 * running in an ecosystem.
 *
 * This implementation of the user store interface gets all of its data from a CouchDB
 * server.
 */
public class CouchdbUserStore implements IUserStore {

    public static final String URL_SCHEMA = "couchdb";

    private URI userStoreUri;
    private CloseableHttpClient httpClient;
    private Log logger;

    public CouchdbUserStore(URI userStoreUri, HttpClientFactory httpClientFactory, LogFactory logFactory) throws UserStoreException {

        // Strip off the 'couchdb:' prefix from the user store URI
        // e.g. couchdb:https://myhost:5984 becomes https://myhost:5984
        try {
            this.userStoreUri = new URI(userStoreUri.toString().replace(URL_SCHEMA + ":", ""));
        } catch (URISyntaxException e) {
            // TODO: Add a custom error message to this exception
            throw new UserStoreException();
        }

        this.logger = logFactory.getLog(getClass());
        this.httpClient = httpClientFactory.createClient();
    }

    @Override
    public List<AuthToken> getTokens() throws UserStoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getTokens'");
    }

    @Override
    public void shutdown() throws UserStoreException {
        try {
            httpClient.close();
        } catch (IOException e) {
            // TODO: Add a custom error message to this exception
            throw new UserStoreException();
        }
    }
}
