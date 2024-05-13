/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.extensions.common.couchdb;

import org.apache.http.impl.client.CloseableHttpClient;

import dev.galasa.extensions.common.impl.HttpRequestFactory;

import java.net.URI;

public interface CouchdbValidator {
    public void checkCouchdbDatabaseIsValid(URI couchdbUri, CloseableHttpClient httpClient, HttpRequestFactory requestFactory) throws CouchdbException;
}
