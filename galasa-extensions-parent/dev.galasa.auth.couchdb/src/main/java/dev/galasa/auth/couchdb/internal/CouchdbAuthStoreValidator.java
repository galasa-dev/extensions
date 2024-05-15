/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb.internal;

import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dev.galasa.extensions.common.couchdb.CouchdbBaseValidator;
import dev.galasa.extensions.common.couchdb.CouchdbException;
import dev.galasa.extensions.common.api.HttpRequestFactory;

public class CouchdbAuthStoreValidator extends CouchdbBaseValidator {

    private final Log logger = LogFactory.getLog(getClass());

    @Override
    public void checkCouchdbDatabaseIsValid(URI couchdbUri, CloseableHttpClient httpClient, HttpRequestFactory httpRequestFactory) throws CouchdbException {
        // Perform the base CouchDB checks
        super.checkCouchdbDatabaseIsValid(couchdbUri, httpClient, httpRequestFactory);

        try {
            checkDatabasePresent(httpClient, couchdbUri, 1, CouchdbAuthStore.TOKENS_DATABASE_NAME);

            logger.debug("Auth Store CouchDB at " + couchdbUri.toString() + " validated");
        } catch (Exception e) {
            throw new CouchdbException("Validation failed", e);
        }
    }
}
