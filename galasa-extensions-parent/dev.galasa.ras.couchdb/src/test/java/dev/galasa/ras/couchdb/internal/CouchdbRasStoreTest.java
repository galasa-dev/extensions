/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import java.net.URI;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import dev.galasa.ras.couchdb.internal.mocks.*;

import com.google.gson.Gson;

import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IRun;
import dev.galasa.framework.spi.utils.GalasaGsonBuilder;
import dev.galasa.ras.couchdb.internal.pojos.PutPostResponse;

public class CouchdbRasStoreTest {
    
    @Rule
    public TestName testName = new TestName();
    

    // Creating the Ras store causes the test structure in the couchdb 
    @Test
    public void TestCanCreateCouchdbRasStoreOK() throws CouchdbRasException {

        // Given...

        IRun mockIRun = new MockIRun("L10");

        IFramework mockFramework = new MockFramework() {
            @Override
            public IRun getTestRun() {
                return mockIRun;
            }      
        };

        URI rasURI = URI.create("http://my.uri");

        MockStatusLine statusLine = new MockStatusLine();
        statusLine.setStatusCode(HttpStatus.SC_CREATED);

        String documentId = "xyz127";
        String documentRev = "123";

        PutPostResponse responseTransportBean = new PutPostResponse();
        responseTransportBean.id = documentId;
        responseTransportBean.ok = true ;
        responseTransportBean.rev = documentRev;

        Gson gson = GalasaGsonBuilder.build();
        String updateMessagePayload = gson.toJson(responseTransportBean);

        HttpEntity entity = new MockHttpEntity(updateMessagePayload); 

        MockCloseableHttpResponse response = new MockCloseableHttpResponse();
        response.setStatusLine(statusLine);
        response.setEntity(entity);

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(response);

        MockCouchdbValidator mockValidator = new MockCouchdbValidator();

        MockHttpClientFactory mockHttpClientFactory = new MockHttpClientFactory(mockHttpClient);
        
        new CouchdbRasStore(mockFramework, rasURI, mockHttpClientFactory, mockValidator);
    }
}