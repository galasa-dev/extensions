package dev.galasa.ras.couchdb.internal;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;


import dev.galasa.framework.spi.IFramework;
import dev.galasa.ras.couchdb.internal.dependencies.api.HttpClientFactory;


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

