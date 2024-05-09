/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.extensions.mocks.couchdb;

import java.net.URI;

import org.apache.http.impl.client.CloseableHttpClient;

import dev.galasa.extensions.common.couchdb.CouchdbAuthStoreException;
import dev.galasa.extensions.common.couchdb.CouchdbValidator;
import dev.galasa.extensions.common.impl.HttpRequestFactory;

public class MockCouchdbValidator implements CouchdbValidator {

    @Override
    public void checkCouchdbDatabaseIsValid(URI rasUri, CloseableHttpClient httpClient, HttpRequestFactory requestFactory) throws CouchdbAuthStoreException {
        // Do nothing.
    }

}
