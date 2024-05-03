/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import dev.galasa.auth.couchdb.internal.CouchdbUserStore;
import dev.galasa.auth.couchdb.internal.CouchdbUserStoreRegistration;
import dev.galasa.extensions.mocks.MockFrameworkInitialisation;
import dev.galasa.extensions.mocks.MockHttpClientFactory;
import dev.galasa.extensions.mocks.MockLogFactory;
import dev.galasa.framework.spi.auth.IUserStore;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class TestCouchdbUserStoreRegistration {

    @Rule
    public TestName testName = new TestName();

    @Test
    public void TestCanCreateARegistrationOK() {
        new CouchdbUserStoreRegistration();
    }

    @Test
    public void TestCanInitialiseARegistrationOK() throws Exception {
        // Given...
        URI uri = new URI("couchdb:https://my.server:5984");
        CouchdbUserStoreRegistration registration = new CouchdbUserStoreRegistration(
                new MockHttpClientFactory(null),
                new MockLogFactory());

        MockFrameworkInitialisation mockFrameworkInit = new MockFrameworkInitialisation(null, uri);

        // When...
        registration.initialise(mockFrameworkInit);

        // Then...
        List<IUserStore> stores = mockFrameworkInit.getRegisteredUserStores();
        assertThat(stores).isNotNull().hasSize(1);
        assertThat(stores.get(0)).isInstanceOf(CouchdbUserStore.class);
    }

    @Test
    public void TestInitialiseARegistrationWithWrongSchemaGetsIgnoredSilently() throws Exception {
        // Given...
        // Wrong schema in this URL.
        URI uri = new URI("notcouchdb:http://my.server/blah");
        CouchdbUserStoreRegistration registration = new CouchdbUserStoreRegistration();

        MockFrameworkInitialisation mockFrameworkInit = new MockFrameworkInitialisation(null, uri);

        // When...
        registration.initialise(mockFrameworkInit);

        // Then...
        // The user store shouldn't have been added to the list of registered user stores
        List<IUserStore> stores = mockFrameworkInit.getRegisteredUserStores();
        assertThat(stores).isNotNull().hasSize(0);
    }
}
