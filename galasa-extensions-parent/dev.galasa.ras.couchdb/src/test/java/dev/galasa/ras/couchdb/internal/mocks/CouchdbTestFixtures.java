/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal.mocks;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IRun;
import dev.galasa.framework.spi.utils.GalasaGsonBuilder;
import dev.galasa.ras.couchdb.internal.CouchdbRasStore;
import dev.galasa.ras.couchdb.internal.pojos.PutPostResponse;

public class CouchdbTestFixtures {

    public static final String rasUriStr = "http://my.uri";
    public static final URI rasUri = URI.create(rasUriStr);

    public static final String documentId1 = "xyz127";
    public static final String documentRev1 = "123";
    public static final String runName1 = "L10";
    public static final String ATTACHMENT_CONTENT1 = "Hello World";
    public static final String ARTIFACT_DOCUMENT_ID_1 = "987-artifact-doc-id-1";
    public static final String ARTIFACT_DOCUMENT_REV_1 = "987-artifact-doc-id-1-v1";

        
    public abstract static class BaseHttpInteraction implements HttpInteraction {

        private String rasUriStr ;
        private String documentId;
        private String documentRev;

        public BaseHttpInteraction(String basicRasUriStr, String documentId, String documentRev ) {
            this.rasUriStr = basicRasUriStr;
            this.documentId = documentId;
            this.documentRev = documentRev;
        }

        public String getDocumentId() {
            return this.documentId;
        }

        public String getDocumentRev() {
            return this.documentRev;
        }

        public String getRasUriStr() {
            return this.rasUriStr;
        }

        public String getExpectedHttpContentType() {
            return "application/json";
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            String hostGot = host.toString();
            assertThat(hostGot).isEqualTo(rasUriStr);

            validateRequestContentType(request);
        }

        public void validateRequestContentType(HttpRequest request) {
            assertThat(request.containsHeader("Content-Type")).as("Missing Content-Type header!").isTrue();
            assertThat(request.getHeaders("Content-Type")[0].getValue()).isEqualTo(getExpectedHttpContentType());
        }

    }


    public static class CreateTestDocInteractionOK extends BaseHttpInteraction {

        public CreateTestDocInteractionOK(String rasUriStr, String documentId, String documentRev ) {
            super(rasUriStr, documentId, documentRev);
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("POST");
            assertThat(request.getRequestLine().getUri()).isEqualTo(getRasUriStr()+"/galasa_run");
        }

        @Override
        public MockCloseableHttpResponse getResponse() {

            // We expect a request to the couchdb system.
            // We will reply with a PutPostResponse

            PutPostResponse responseTransportBean = new PutPostResponse();
            responseTransportBean.id = getDocumentId();
            responseTransportBean.ok = true ;
            responseTransportBean.rev = getDocumentRev();

            Gson gson = GalasaGsonBuilder.build();
            String updateMessagePayload = gson.toJson(responseTransportBean);

            HttpEntity entity = new MockHttpEntity(updateMessagePayload); 

            MockCloseableHttpResponse response = new MockCloseableHttpResponse();

            MockStatusLine statusLine = new MockStatusLine();
            statusLine.setStatusCode(HttpStatus.SC_CREATED);
            response.setStatusLine(statusLine);
            response.setEntity(entity);

            return response;
        }
    }

    public static class CreateArtifactDocInteractionOK extends BaseHttpInteraction {

        public CreateArtifactDocInteractionOK(String rasUriStr, String documentId, String documentRev ) {
            super(rasUriStr, documentId, documentRev);
        }


        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("POST");
            assertThat(request.getRequestLine().getUri()).isEqualTo(getRasUriStr()+"/galasa_artifacts");

            HttpEntity entity = ((HttpEntityEnclosingRequest)request).getEntity();
            String content ;
            try {
                content = EntityUtils.toString(entity);
            } catch (IOException ex ) {
                throw new RuntimeException("Failed to read content from request."+ request.getRequestLine().getUri());
            }
            assertThat(content).contains("\"runId\": \""+CouchdbTestFixtures.documentId1);
            assertThat(content).contains("\"runName\": \""+CouchdbTestFixtures.runName1);
        }

        @Override
        public MockCloseableHttpResponse getResponse() {

            // We expect a request to the couchdb system.
            // We will reply with a PutPostResponse

            PutPostResponse responseTransportBean = new PutPostResponse();
            responseTransportBean.id = getDocumentId();
            responseTransportBean.ok = true ;
            responseTransportBean.rev = getDocumentRev();

            Gson gson = GalasaGsonBuilder.build();
            String updateMessagePayload = gson.toJson(responseTransportBean);

            HttpEntity entity = new MockHttpEntity(updateMessagePayload); 

            MockCloseableHttpResponse response = new MockCloseableHttpResponse();

            MockStatusLine statusLine = new MockStatusLine();
            statusLine.setStatusCode(HttpStatus.SC_CREATED);
            response.setStatusLine(statusLine);
            response.setEntity(entity);

            return response;
        }
    }


    public CouchdbRasStore createCouchdbRasStore(Map<String,String> inputProps) throws Exception {
        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();
        return createCouchdbRasStore(inputProps,interactions, new MockLogFactory());
    }
    
    public CouchdbRasStore createCouchdbRasStore(Map<String,String> inputProps, List<HttpInteraction> extraInteractions, MockLogFactory logFactory ) throws Exception {

        if (logFactory == null ) {
            logFactory = new MockLogFactory();
        }

        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();

        Map<String,String> props = new HashMap<String,String>();
        if (inputProps != null) {
            props.putAll(inputProps);
        }

        MockConfigurationPropertyStoreService mockCps = new MockConfigurationPropertyStoreService(props);

        IRun mockIRun = new MockIRun(runName1);

        IFramework mockFramework = new MockFramework() {
            @Override
            public IRun getTestRun() {
                return mockIRun;
            }     
            @Override
            public @NotNull IConfigurationPropertyStoreService getConfigurationPropertyService(
                    @NotNull String namespace) throws ConfigurationPropertyStoreException {
                assertThat(namespace).isEqualTo("couchdb");
                return mockCps;
            } 
        };

        

        interactions.add( new CreateTestDocInteractionOK(rasUriStr, documentId1, documentRev1) );
        interactions.add( new CreateArtifactDocInteractionOK(rasUriStr, documentId1, documentRev1) );
        interactions.addAll(extraInteractions);

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        MockCouchdbValidator mockValidator = new MockCouchdbValidator();

        MockHttpClientFactory mockHttpClientFactory = new MockHttpClientFactory(mockHttpClient);
        
        URI rasURI = URI.create(rasUriStr);
        CouchdbRasStore couchdbRasStore = new CouchdbRasStore(mockFramework, rasURI, mockHttpClientFactory, mockValidator, logFactory);

        return couchdbRasStore;
    }
}
