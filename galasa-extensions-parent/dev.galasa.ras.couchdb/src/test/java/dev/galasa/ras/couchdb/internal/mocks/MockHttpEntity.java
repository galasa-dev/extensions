/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal.mocks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import java.io.ByteArrayInputStream;



public class MockHttpEntity implements HttpEntity {

    private MockHttpHeader contentTypeJsonHeader ;
    private byte[] payloadMessageBytes ;

    public MockHttpEntity(String payloadMessage) {

        contentTypeJsonHeader = new MockHttpHeader("Content-Type","application/json");

        payloadMessageBytes = payloadMessage.getBytes();
    }

    @Override
    public void consumeContent() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'consumeContent'");
    }

    @Override
    public Header getContentEncoding() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getContentEncoding'");
    }

    @Override
    public Header getContentType() {
        return this.contentTypeJsonHeader;
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

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        return new ByteArrayInputStream(this.payloadMessageBytes);
    }

    @Override
    public long getContentLength() {
        return this.payloadMessageBytes.length;
    }
}

