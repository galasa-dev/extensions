/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import org.apache.http.impl.client.CloseableHttpClient;
import java.net.URI;

public interface CouchdbValidator {
    public void checkCouchdbDatabaseIsValid( URI rasUri, CloseableHttpClient httpClient ) throws CouchdbRasException ;
}
