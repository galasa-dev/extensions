/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.io.ByteArrayInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class CouchdbValidatorImplTest {
    
    @Rule
    public TestName testName = new TestName();


    @Test
    public void TestRasStoreCreateBlowsUpIfCouchDBDoesntReturnWelcomeString() throws Exception {


        // Given...
        URI rasURI = URI.create("http://my.uri");

        StatusLine statusLine = new MockStatusLine();

        MockHttpHeader contentTypeJsonHeader = new MockHttpHeader("Content-Type","application/json",null);

        String welcomeToCouchDBMessage = "{" +
            "\"couchdb\":\"dummy-edition\","+
            "\"version\":\"2.3.1\""+
        "}";

        byte[] welcomeToCouchDBMessageBytes = welcomeToCouchDBMessage.getBytes();

        HttpEntity entity = new MockHttpEntity() {
            @Override
            public Header getContentType() {
                return contentTypeJsonHeader ;
            }

            @Override
            public InputStream getContent() throws IOException, UnsupportedOperationException {
                return new ByteArrayInputStream(welcomeToCouchDBMessageBytes);
            }

            @Override
            public long getContentLength() {
                return welcomeToCouchDBMessageBytes.length;
            }
        };

        MockCloseableHttpResponse response = new MockCloseableHttpResponse();
        response.setStatusLine(statusLine);
        response.setEntity(entity);

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient() {
            @Override
            protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context)
                throws IOException, ClientProtocolException {
                return response;
            }
        };


        CouchdbValidator validatorUnderTest = new CouchdbValidatorImpl();

        // When..
        Throwable thrown = catchThrowable(()-> validatorUnderTest.checkCouchdbDatabaseIsValid( rasURI , mockHttpClient ));

        // Then..
        assertThat(thrown).isInstanceOf(CouchdbRasException.class);
    }
}