package dev.galasa.ras.couchdb.internal;

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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class MockStatusLine implements StatusLine {

    // Status code defaults to OK.
    private int code = HttpStatus.SC_OK;

    @Override
    public ProtocolVersion getProtocolVersion() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProtocolVersion'");
    }

    @Override
    public String getReasonPhrase() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReasonPhrase'");
    }

    public void setStatusCode(int newCode) {
        this.code = newCode ;
    }

    @Override
    public int getStatusCode() {
        return this.code;
    }
    
}