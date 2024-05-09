/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb.internal;

import java.net.*;

import org.osgi.service.component.annotations.Component;

import dev.galasa.extensions.common.api.HttpClientFactory;
import dev.galasa.extensions.common.api.LogFactory;
import dev.galasa.extensions.common.impl.HttpClientFactoryImpl;
import dev.galasa.extensions.common.impl.LogFactoryImpl;
import dev.galasa.framework.spi.IFrameworkInitialisation;
import dev.galasa.framework.spi.auth.IAuthStoreRegistration;
import dev.galasa.framework.spi.auth.AuthStoreException;

@Component(service = { IAuthStoreRegistration.class })
public class CouchdbAuthStoreRegistration implements IAuthStoreRegistration {

    private HttpClientFactory httpClientFactory;
    private LogFactory logFactory;

    public CouchdbAuthStoreRegistration() {
        this(new HttpClientFactoryImpl(), new LogFactoryImpl());
    }

    public CouchdbAuthStoreRegistration(HttpClientFactory httpClientFactory, LogFactory logFactory) {
        this.httpClientFactory = httpClientFactory;
        this.logFactory = logFactory;
    }

    /**
     * This method checks that the auth store is a remote URL reference, and if true
     * registers a new couchdb auth store as the only auth store.
     *
     * @param frameworkInitialisation Parameters this extension can use to to
     *                                initialise itself.
     * @throws AuthStoreException if there was a problem accessing the auth store.
     */
    @Override
    public void initialise(IFrameworkInitialisation frameworkInitialisation) throws AuthStoreException {

        URI authStoreUri = frameworkInitialisation.getAuthStoreUri();

        if (isUriRefferringToThisExtension(authStoreUri)) {
            frameworkInitialisation
                    .registerAuthStore(new CouchdbAuthStore(authStoreUri, httpClientFactory, logFactory));
        }
    }

    /**
     * Checks whether the provided URI to the auth store is a couchdb URI or not.
     *
     * @param uri - URI to the auth store of the form
     *            "couchdb:http://my.couchdb.server:5984"
     * @return true if the URI is a couchdb URI, false otherwise.
     */
    public boolean isUriRefferringToThisExtension(URI uri) {
        return CouchdbAuthStore.URL_SCHEMA.equals(uri.getScheme());
    }
}