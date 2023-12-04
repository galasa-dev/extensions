/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import java.io.IOException;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

public class MockCloseableHttpClient extends CloseableHttpClient {

    @Override
    public HttpParams getParams() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getParams'");
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getConnectionManager'");
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'close'");
    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost target, org.apache.http.HttpRequest request, HttpContext context)
            throws IOException, ClientProtocolException {
        return new MockCloseableHttpResponse();
    }

}
