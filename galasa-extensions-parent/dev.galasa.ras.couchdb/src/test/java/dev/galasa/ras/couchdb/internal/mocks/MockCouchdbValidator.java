/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal.mocks;

import java.net.URI;

import org.apache.http.impl.client.CloseableHttpClient;

import dev.galasa.extensions.common.api.HttpRequestFactory;
import dev.galasa.extensions.common.couchdb.CouchdbException;
import dev.galasa.extensions.common.couchdb.CouchdbValidator;

public class MockCouchdbValidator implements CouchdbValidator {

    @Override
    public void checkCouchdbDatabaseIsValid(URI rasUri, CloseableHttpClient httpClient, HttpRequestFactory requestFactory) throws CouchdbException {
        // Do nothing.
    }

}
