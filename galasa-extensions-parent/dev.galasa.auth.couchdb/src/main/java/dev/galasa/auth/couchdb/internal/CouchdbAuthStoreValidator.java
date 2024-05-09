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

import dev.galasa.extensions.common.couchdb.CouchdbAuthStoreException;
import dev.galasa.extensions.common.couchdb.CouchdbBaseValidator;
import dev.galasa.extensions.common.impl.HttpRequestFactory;

public class CouchdbAuthStoreValidator extends CouchdbBaseValidator {

    private final Log logger = LogFactory.getLog(getClass());

    @Override
    public void checkCouchdbDatabaseIsValid(URI couchdbUri, CloseableHttpClient httpClient, HttpRequestFactory httpRequestFactory) throws CouchdbAuthStoreException {
        super.checkCouchdbDatabaseIsValid(couchdbUri, httpClient, httpRequestFactory);

        try {
            checkDatabasePresent(httpClient, couchdbUri, 1, "galasa_tokens");

            logger.debug("Auth Store CouchDB at " + couchdbUri.toString() + " validated");
        } catch (Exception e) {
            throw new CouchdbAuthStoreException("Validation failed", e);
        }
    }
}
