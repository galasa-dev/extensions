package dev.galasa.ras.couchdb.internal;

import java.io.IOException;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.params.HttpParams;

public class MockCloseableHttpResponse implements CloseableHttpResponse {

    boolean isClosed = false ;
    StatusLine statusLine ;
    HttpEntity entity ;

    @Override
    public HttpEntity getEntity() {
        return this.entity;
    }

    @Override
    public Locale getLocale() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLocale'");
    }

    @Override
    public StatusLine getStatusLine() {
        return this.statusLine;
    }

    @Override
    public void setEntity(HttpEntity newEntity) {
        this.entity = newEntity;
    }

    @Override
    public void setLocale(Locale arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setLocale'");
    }

    @Override
    public void setReasonPhrase(String arg0) throws IllegalStateException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setReasonPhrase'");
    }

    @Override
    public void setStatusCode(int arg0) throws IllegalStateException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStatusCode'");
    }

    @Override
    public void setStatusLine(StatusLine newStatusLine) {
        this.statusLine = newStatusLine;
    }

    @Override
    public void setStatusLine(ProtocolVersion arg0, int arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStatusLine'");
    }

    @Override
    public void setStatusLine(ProtocolVersion arg0, int arg1, String arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStatusLine'");
    }

    @Override
    public void addHeader(Header arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addHeader'");
    }

    @Override
    public void addHeader(String arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addHeader'");
    }

    @Override
    public boolean containsHeader(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'containsHeader'");
    }

    @Override
    public Header[] getAllHeaders() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllHeaders'");
    }

    @Override
    public Header getFirstHeader(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFirstHeader'");
    }

    @Override
    public Header[] getHeaders(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getHeaders'");
    }

    @Override
    public Header getLastHeader(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLastHeader'");
    }

    @Override
    public HttpParams getParams() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getParams'");
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProtocolVersion'");
    }

    @Override
    public HeaderIterator headerIterator() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'headerIterator'");
    }

    @Override
    public HeaderIterator headerIterator(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'headerIterator'");
    }

    @Override
    public void removeHeader(Header arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeHeader'");
    }

    @Override
    public void removeHeaders(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeHeaders'");
    }

    @Override
    public void setHeader(Header arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setHeader'");
    }

    @Override
    public void setHeader(String arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setHeader'");
    }

    @Override
    public void setHeaders(Header[] arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setHeaders'");
    }

    @Override
    public void setParams(HttpParams arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setParams'");
    }

    @Override
    public void close() throws IOException {
        isClosed = true ;
    }

}

