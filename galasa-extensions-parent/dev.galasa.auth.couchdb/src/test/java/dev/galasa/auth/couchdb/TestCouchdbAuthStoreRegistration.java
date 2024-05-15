/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb;

import org.junit.Test;

import dev.galasa.auth.couchdb.internal.CouchdbAuthStore;
import dev.galasa.auth.couchdb.internal.CouchdbAuthStoreRegistration;
import dev.galasa.extensions.common.impl.HttpRequestFactoryImpl;
import dev.galasa.extensions.mocks.MockFrameworkInitialisation;
import dev.galasa.extensions.mocks.MockHttpClientFactory;
import dev.galasa.extensions.mocks.MockLogFactory;
import dev.galasa.extensions.mocks.couchdb.MockCouchdbValidator;
import dev.galasa.framework.spi.auth.IAuthStore;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class TestCouchdbAuthStoreRegistration {

    @Test
    public void TestCanCreateARegistrationOK() {
        new CouchdbAuthStoreRegistration();
    }

    @Test
    public void TestCanInitialiseARegistrationOK() throws Exception {
        // Given...
        URI uri = new URI("couchdb:https://my.server:5984");
        CouchdbAuthStoreRegistration registration = new CouchdbAuthStoreRegistration(
                new MockHttpClientFactory(null),
                new HttpRequestFactoryImpl(),
                new MockLogFactory(),
                new MockCouchdbValidator());

        MockFrameworkInitialisation mockFrameworkInit = new MockFrameworkInitialisation(null, uri);

        // When...
        registration.initialise(mockFrameworkInit);

        // Then...
        List<IAuthStore> stores = mockFrameworkInit.getRegisteredAuthStores();
        assertThat(stores).isNotNull().hasSize(1);
        assertThat(stores.get(0)).isInstanceOf(CouchdbAuthStore.class);
    }

    @Test
    public void TestInitialiseARegistrationWithWrongSchemaGetsIgnoredSilently() throws Exception {
        // Given...
        // Wrong schema in this URL.
        URI uri = new URI("notcouchdb:http://my.server/blah");
        CouchdbAuthStoreRegistration registration = new CouchdbAuthStoreRegistration();

        MockFrameworkInitialisation mockFrameworkInit = new MockFrameworkInitialisation(null, uri);

        // When...
        registration.initialise(mockFrameworkInit);

        // Then...
        // The auth store shouldn't have been added to the list of registered user
        // stores
        List<IAuthStore> stores = mockFrameworkInit.getRegisteredAuthStores();
        assertThat(stores).isNotNull().hasSize(0);
    }
}
