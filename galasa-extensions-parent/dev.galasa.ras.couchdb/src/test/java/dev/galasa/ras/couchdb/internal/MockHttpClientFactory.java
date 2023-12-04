package dev.galasa.ras.couchdb.internal;

import org.apache.http.impl.client.CloseableHttpClient;

import dev.galasa.ras.couchdb.internal.dependencies.api.HttpClientFactory;

public class MockHttpClientFactory implements HttpClientFactory {
    private CloseableHttpClient clientToReturn ;

    public MockHttpClientFactory(CloseableHttpClient clientToReturn) {
        this.clientToReturn = clientToReturn;
    }

    @Override
    public CloseableHttpClient createClient() {
        return this.clientToReturn;
    }
}