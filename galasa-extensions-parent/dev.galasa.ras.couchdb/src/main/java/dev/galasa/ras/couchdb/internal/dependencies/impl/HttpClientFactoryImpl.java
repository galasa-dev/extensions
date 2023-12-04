/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal.dependencies.impl;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import dev.galasa.ras.couchdb.internal.dependencies.api.HttpClientFactory;

public class HttpClientFactoryImpl implements HttpClientFactory {

    @Override
    public CloseableHttpClient createClient() {
        return HttpClients.createDefault();
    }

}
