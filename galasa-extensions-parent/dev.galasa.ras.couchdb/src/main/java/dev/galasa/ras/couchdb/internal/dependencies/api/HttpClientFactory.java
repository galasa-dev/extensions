/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal.dependencies.api;

import org.apache.http.impl.client.CloseableHttpClient;

public interface HttpClientFactory {
    CloseableHttpClient createClient();
}
