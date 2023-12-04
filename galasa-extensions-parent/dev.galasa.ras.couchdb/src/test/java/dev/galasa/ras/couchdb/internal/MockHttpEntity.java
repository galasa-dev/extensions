/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;


public class MockHttpEntity implements HttpEntity {

    @Override
    public void consumeContent() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'consumeContent'");
    }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getContent'");
    }

    @Override
    public Header getContentEncoding() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getContentEncoding'");
    }

    @Override
    public long getContentLength() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getContentLength'");
    }

    @Override
    public Header getContentType() {
        throw new UnsupportedOperationException("Unimplemented method 'getContentType'");
    }

    @Override
    public boolean isChunked() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isChunked'");
    }

    @Override
    public boolean isRepeatable() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isRepeatable'");
    }

    @Override
    public boolean isStreaming() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isStreaming'");
    }

    @Override
    public void writeTo(OutputStream arg0) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'writeTo'");
    }

}

