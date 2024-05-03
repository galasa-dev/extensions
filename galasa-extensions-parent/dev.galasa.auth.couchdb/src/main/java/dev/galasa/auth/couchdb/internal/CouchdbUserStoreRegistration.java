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
import dev.galasa.framework.spi.auth.IUserStoreRegistration;
import dev.galasa.framework.spi.auth.UserStoreException;

@Component(service = { IUserStoreRegistration.class })
public class CouchdbUserStoreRegistration implements IUserStoreRegistration {

    private HttpClientFactory httpClientFactory;
    private LogFactory logFactory;

    public CouchdbUserStoreRegistration() {
        this(new HttpClientFactoryImpl(), new LogFactoryImpl());
    }

    public CouchdbUserStoreRegistration(HttpClientFactory httpClientFactory, LogFactory logFactory) {
        this.httpClientFactory = httpClientFactory;
        this.logFactory = logFactory;
    }

    /**
     * This method checks that the user store is a remote URL reference, and if true
     * registers a new couchdb user store as the only user store.
     *
     * @param frameworkInitialisation Parameters this extension can use to to
     *                                initialise itself.
     * @throws UserStoreException if there was a problem accessing the user store.
     */
    @Override
    public void initialise(IFrameworkInitialisation frameworkInitialisation) throws UserStoreException {

        URI userStoreUri = frameworkInitialisation.getUserStoreUri();

        if (isUriRefferringToThisExtension(userStoreUri)) {
            frameworkInitialisation.registerUserStore(new CouchdbUserStore(userStoreUri, httpClientFactory, logFactory));
        }
    }

    /**
     * Checks whether the provided URI to the user store is a couchdb URI or not.
     *
     * @param uri - URI to the user store of the form
     *            "couchdb:http://my.couchdb.server:5984"
     * @return true if the URI is a couchdb URI, false otherwise.
     */
    public boolean isUriRefferringToThisExtension(URI uri) {
        return CouchdbUserStore.URL_SCHEMA.equals(uri.getScheme());
    }
}
