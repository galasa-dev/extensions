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
import dev.galasa.framework.spi.auth.IAuthStore;
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

    private URI authStoreUri;
    private CloseableHttpClient httpClient;
    private Log logger;

    public CouchdbAuthStore(URI authStoreUri, HttpClientFactory httpClientFactory, LogFactory logFactory)
            throws AuthStoreException {

        // Strip off the 'couchdb:' prefix from the auth store URI
        // e.g. couchdb:https://myhost:5984 becomes https://myhost:5984
        try {
            this.authStoreUri = new URI(authStoreUri.toString().replace(URL_SCHEMA + ":", ""));
        } catch (URISyntaxException e) {
            // TODO-EM: Add a custom error message to this exception
            throw new AuthStoreException();
        }

        this.logger = logFactory.getLog(getClass());
        this.httpClient = httpClientFactory.createClient();

        // TODO-EM: Check that the couchdb database is valid
    }

    @Override
    public List<AuthToken> getTokens() throws AuthStoreException {
        throw new UnsupportedOperationException("Unimplemented method 'getTokens'");
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
