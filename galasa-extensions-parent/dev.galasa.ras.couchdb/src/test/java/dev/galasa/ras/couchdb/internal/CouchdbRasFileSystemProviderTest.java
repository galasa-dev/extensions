/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.gson.Gson;

import dev.galasa.framework.spi.utils.GalasaGsonBuilder;
import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures;
import dev.galasa.ras.couchdb.internal.mocks.HttpInteraction;
import dev.galasa.ras.couchdb.internal.mocks.MockCloseableHttpResponse;
import dev.galasa.ras.couchdb.internal.mocks.MockHttpEntity;
import dev.galasa.ras.couchdb.internal.mocks.MockLogFactory;
import dev.galasa.ras.couchdb.internal.mocks.MockStatusLine;
import dev.galasa.ras.couchdb.internal.pojos.PutPostResponse;
import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures.BaseHttpInteraction;



public class CouchdbRasFileSystemProviderTest {
    @Rule
    public TestName testName = new TestName();

    CouchdbTestFixtures fixtures = new CouchdbTestFixtures();

    public static class  PutArtifactInteraction extends BaseHttpInteraction {

        String testFileNameToCreate;

        public PutArtifactInteraction(String rasUriStr , String documentId, String documentRev, String testFileNameToCreate) {
            super(rasUriStr, documentId, documentRev);
            this.testFileNameToCreate = testFileNameToCreate;
        }

        @Override
        public String getExpectedHttpContentType() {
            return "plain/text";
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("PUT");
            assertThat(request.getRequestLine().getUri()).isEqualTo(getRasUriStr()+"/galasa_artifacts/"+getDocumentId()+"/%2F"+this.testFileNameToCreate);

            // Check that the headers have been set up.
            assertThat(request.getHeaders("If-Match")).isNotNull();
            assertThat(request.getHeaders("If-Match")[0].getValue()).isEqualTo(CouchdbTestFixtures.documentRev1);

            assertThat(request.getHeaders("Accept")).isNotNull();
            assertThat(request.getHeaders("Accept")[0].getValue()).isEqualTo("application/json");

            // Check that the entity contains the attachment string.
            HttpEntity entity = ((HttpEntityEnclosingRequest)request).getEntity();
            String content ;
            try {
                content = EntityUtils.toString(entity);
            } catch (IOException ex ) {
                throw new RuntimeException("Failed to read content from request."+ request.getRequestLine().getUri());
            }
            assertThat(content).isEqualTo(CouchdbTestFixtures.ATTACHMENT_CONTENT1);
        }

        @Override
        public MockCloseableHttpResponse getResponse() {

            PutPostResponse responseTransportBean = new PutPostResponse();
            responseTransportBean.id = CouchdbTestFixtures.ARTIFACT_DOCUMENT_ID_1;
            responseTransportBean.ok = true ;
            responseTransportBean.rev = CouchdbTestFixtures.ARTIFACT_DOCUMENT_REV_1;

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

    @Test 
    public void TestFileCloseCausesCouchDBArtifactToBeSaved() throws Exception {

        // Given...
        String fileContent = CouchdbTestFixtures.ATTACHMENT_CONTENT1 ;

        String testFileNameToCreate = testName.getMethodName();

        // We expect an http interaction shortly, where a PUT is sent to couchdb...
        // http://my.uri/galasa_artifacts/xyz127/%2FTestFileCloseCausesCouchDBArtifactToBeSaved 
        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();
        interactions.add( new  PutArtifactInteraction( CouchdbTestFixtures.rasUriStr , CouchdbTestFixtures.documentId1, CouchdbTestFixtures.documentRev1, testFileNameToCreate) ) ;

        MockLogFactory mockLogFactory = new MockLogFactory();
        CouchdbRasStore couchdbStore = fixtures.createCouchdbRasStore(null,interactions, mockLogFactory);

        // When... we write a file to the couchdb filestore...
        Path rootDirPath = couchdbStore.getStoredArtifactsRoot();
        Path testFilePath = rootDirPath.resolve(testFileNameToCreate);

        Files.write(testFilePath, fileContent.getBytes(), StandardOpenOption.CREATE);

        String logContent = mockLogFactory.toString();
        assertThat(logContent).isNotEmpty();
    }
    
}
