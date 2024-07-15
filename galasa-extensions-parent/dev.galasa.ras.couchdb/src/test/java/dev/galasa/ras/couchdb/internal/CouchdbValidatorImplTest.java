/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import java.util.*;
import static org.assertj.core.api.Assertions.*;

import org.apache.http.*;
import org.junit.*;
import org.junit.rules.TestName;

import dev.galasa.framework.spi.utils.GalasaGson;
import dev.galasa.extensions.common.couchdb.pojos.Welcome;
import dev.galasa.extensions.common.impl.HttpRequestFactoryImpl;
import dev.galasa.extensions.common.api.HttpRequestFactory;
import dev.galasa.extensions.mocks.*;
import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures;
import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures.BaseHttpInteraction;;

public class CouchdbValidatorImplTest {
    
    @Rule
    public TestName testName = new TestName();



    public static class WelcomeInteractionOK extends BaseHttpInteraction {

        public WelcomeInteractionOK(String rasUriStr, String documentId, String documentRev ) {
            super(rasUriStr, documentId, "124");
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("GET");
        }


        @Override
        public void validateRequestContentType(HttpRequest request) {
            // We don't expect a Content-type header as there is no payload sent to the server
        }

        @Override
        public MockCloseableHttpResponse getResponse() {

            // We expect a request to the couchdb system.
            // We will reply with a PutPostResponse

            Welcome welcomeBean = new Welcome();
            welcomeBean.couchdb = "dummy-edition";
            welcomeBean.version = "3.3.3";

            GalasaGson gson = new GalasaGson();
            String updateMessagePayload = gson.toJson(welcomeBean);

            HttpEntity entity = new MockHttpEntity(updateMessagePayload); 

            MockCloseableHttpResponse response = new MockCloseableHttpResponse();

            MockStatusLine statusLine = new MockStatusLine();
            statusLine.setStatusCode(HttpStatus.SC_CREATED);
            response.setStatusLine(statusLine);
            response.setEntity(entity);

            return response;
        }
    }

    public static class checkDatabasePresentInteraction extends BaseHttpInteraction {

        public checkDatabasePresentInteraction(String rasUriStr, String documentId, String documentRev) {
            super(rasUriStr, documentId, "124");
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("HEAD");
        }


        @Override
        public void validateRequestContentType(HttpRequest request) {
            // We don't expect a Content-type header as there is no payload sent to the server
        }

        @Override
        public MockCloseableHttpResponse getResponse() {

            // We expect a request to the couchdb system.
            // We will reply with a PutPostResponse

            Welcome welcomeBean = new Welcome();
            welcomeBean.couchdb = "dummy-edition";
            welcomeBean.version = "3.3.3";

            GalasaGson gson = new GalasaGson();
            String updateMessagePayload = gson.toJson(welcomeBean);

            HttpEntity entity = new MockHttpEntity(updateMessagePayload); 

            MockCloseableHttpResponse response = new MockCloseableHttpResponse();

            MockStatusLine statusLine = new MockStatusLine();
            statusLine.setStatusCode(HttpStatus.SC_OK);
            response.setStatusLine(statusLine);
            response.setEntity(entity);

            return response;
        }
    }

    public static class checkDatabasePresentNotFoundInteraction extends BaseHttpInteraction {

        public checkDatabasePresentNotFoundInteraction(String rasUriStr, String documentId, String documentRev) {
            super(rasUriStr, documentId, "124");
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("HEAD");
        }


        @Override
        public void validateRequestContentType(HttpRequest request) {
            // We don't expect a Content-type header as there is no payload sent to the server
        }

        @Override
            public MockCloseableHttpResponse getResponse() {

            MockCloseableHttpResponse response = new MockCloseableHttpResponse();

            MockStatusLine statusLine = new MockStatusLine();
            statusLine.setStatusCode(HttpStatus.SC_NOT_FOUND);
            response.setStatusLine(statusLine);

            return response;
        }
    }
    public static class checkDatabasePresentConflictInteraction extends BaseHttpInteraction {

        public checkDatabasePresentConflictInteraction(String rasUriStr, String documentId, String documentRev) {
            super(rasUriStr, documentId, "124");
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("PUT");
        }


        @Override
        public void validateRequestContentType(HttpRequest request) {
            // We don't expect a Content-type header as there is no payload sent to the server
        }

        @Override
            public MockCloseableHttpResponse getResponse() {

            MockCloseableHttpResponse response = new MockCloseableHttpResponse();

            MockStatusLine statusLine = new MockStatusLine();
            statusLine.setStatusCode(HttpStatus.SC_CONFLICT);
            response.setStatusLine(statusLine);

            return response;
        }
    }

    public static class checkDatabaseHasDocumentInteraction extends BaseHttpInteraction {

        public checkDatabaseHasDocumentInteraction(String rasUriStr, String documentId, String documentRev) {
            super(rasUriStr, documentId, "124");
        }
        
        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("GET");
        }

        @Override
        public MockCloseableHttpResponse getResponse() {

            // We expect a request to the couchdb system.
            // We will reply with a PutPostResponse

            Welcome welcomeBean = new Welcome();
            welcomeBean.couchdb = "dummy-edition";
            welcomeBean.version = "3.3.3";

            GalasaGson gson = new GalasaGson();
            String updateMessagePayload = gson.toJson(welcomeBean);

            HttpEntity entity = new MockHttpEntity(updateMessagePayload); 

            MockCloseableHttpResponse response = new MockCloseableHttpResponse();

            MockStatusLine statusLine = new MockStatusLine();
            statusLine.setStatusCode(HttpStatus.SC_OK);
            response.setStatusLine(statusLine);
            response.setEntity(entity);

            return response;
        }

    }
    
    public static class submitDesignDocumentInteraction extends BaseHttpInteraction {

        public submitDesignDocumentInteraction(String rasUriStr, String documentId, String documentRev) {
            super(rasUriStr, documentId, "124");
        }
        
        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("PUT");
        }

        @Override
        public MockCloseableHttpResponse getResponse() {

            // We expect a request to the couchdb system.
            // We will reply with a PutPostResponse

            Welcome welcomeBean = new Welcome();
            welcomeBean.couchdb = "dummy-edition";
            welcomeBean.version = "3.3.3";

            GalasaGson gson = new GalasaGson();
            String updateMessagePayload = gson.toJson(welcomeBean);

            HttpEntity entity = new MockHttpEntity(updateMessagePayload); 

            MockCloseableHttpResponse response = new MockCloseableHttpResponse();

            MockStatusLine statusLine = new MockStatusLine();
            statusLine.setStatusCode(HttpStatus.SC_CREATED);
            response.setStatusLine(statusLine);
            response.setEntity(entity);

            return response;
        }
    }

    public static class checkIndexPOSTInteraction extends BaseHttpInteraction {

        public checkIndexPOSTInteraction(String rasUriStr, String documentId, String documentRev) {
            super(rasUriStr, documentId, "124");
        }
        
        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("POST");
        }

        @Override
        public MockCloseableHttpResponse getResponse() {

            // We expect a request to the couchdb system.
            // We will reply with a PutPostResponse

            Welcome welcomeBean = new Welcome();
            welcomeBean.couchdb = "dummy-edition";
            welcomeBean.version = "3.3.3";

            GalasaGson gson = new GalasaGson();
            String updateMessagePayload = gson.toJson(welcomeBean);

            HttpEntity entity = new MockHttpEntity(updateMessagePayload); 

            MockCloseableHttpResponse response = new MockCloseableHttpResponse();

            MockStatusLine statusLine = new MockStatusLine();
            statusLine.setStatusCode(HttpStatus.SC_OK);
            response.setStatusLine(statusLine);
            response.setEntity(entity);

            return response;
        }

    }

    @Test
    public void TestRasStoreCreateBlowsUpIfCouchDBDoesntReturnWelcomeString() throws Exception {

        List <HttpInteraction> interactions = new ArrayList<HttpInteraction>();

        interactions.add( new WelcomeInteractionOK(
            CouchdbTestFixtures.rasUriStr,
            CouchdbTestFixtures.documentId1, 
            CouchdbTestFixtures.documentRev1 
        ) );

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        CouchdbValidator validatorUnderTest = new CouchdbValidatorImpl();
        HttpRequestFactory requestFactory = new HttpRequestFactoryImpl("Basic", "checkisvalid");

        // When..
        Throwable thrown = catchThrowable(()-> validatorUnderTest.checkCouchdbDatabaseIsValid( CouchdbTestFixtures.rasUri , mockHttpClient, requestFactory));

        // Then..
        assertThat(thrown).isNotNull();
        assertThat(thrown).as("exception caught is of type "+thrown.getClass().toString()).isInstanceOf(CouchdbRasException.class);
    }

    @Test
    public void TestRasStoreCreatesDBIfCouchDBReturnsWelcomeString() throws Exception {
        String rasURI = CouchdbTestFixtures.rasUriStr;
        String docID = CouchdbTestFixtures.documentId1; 
        String docRev = CouchdbTestFixtures.documentRev1;
        List <HttpInteraction> interactions = new ArrayList<HttpInteraction>();

        //Check Welcome Screen
        interactions.add( new WelcomeInteractionOK( rasURI, docID,docRev ){
            @Override
            public MockCloseableHttpResponse getResponse() {
    
                // We expect a request to the couchdb system.
                // We will reply with a PutPostResponse
    
                Welcome welcomeBean = new Welcome();
                welcomeBean.couchdb = "Welcome";
                welcomeBean.version = "3.3.3";
    
                GalasaGson gson = new GalasaGson();
                String updateMessagePayload = gson.toJson(welcomeBean);
    
                HttpEntity entity = new MockHttpEntity(updateMessagePayload); 
    
                MockCloseableHttpResponse response = new MockCloseableHttpResponse();
    
                MockStatusLine statusLine = new MockStatusLine();
                statusLine.setStatusCode(HttpStatus.SC_OK);
                response.setStatusLine(statusLine);
                response.setEntity(entity);

                return response;
            }
        } );

        //Add Interactions for checking the databases are present
        interactions.add( new checkDatabasePresentInteraction( rasURI, docID,docRev ));
        interactions.add( new checkDatabasePresentInteraction( rasURI, docID,docRev ));
        interactions.add( new checkDatabasePresentInteraction( rasURI, docID,docRev ));

        // Check Desing Docs Interactions
        interactions.add( new checkDatabaseHasDocumentInteraction( rasURI, docID,docRev ));
        interactions.add( new submitDesignDocumentInteraction( rasURI, docID,docRev ));

        //Check Indexes Interactions
        interactions.add( new checkDatabaseHasDocumentInteraction( rasURI, docID,docRev ));
        interactions.add( new checkIndexPOSTInteraction( rasURI, docID,docRev ));
        interactions.add( new checkDatabaseHasDocumentInteraction( rasURI, docID,docRev ));
        interactions.add( new checkIndexPOSTInteraction( rasURI, docID,docRev ));
        interactions.add( new checkDatabaseHasDocumentInteraction( rasURI, docID,docRev ));
        interactions.add( new checkIndexPOSTInteraction( rasURI, docID,docRev ));
        interactions.add( new checkDatabaseHasDocumentInteraction( rasURI, docID,docRev ));
        interactions.add( new checkIndexPOSTInteraction( rasURI, docID,docRev ));
        interactions.add( new checkDatabaseHasDocumentInteraction( rasURI, docID,docRev ));
        interactions.add( new checkIndexPOSTInteraction( rasURI, docID,docRev ));
        interactions.add( new checkDatabaseHasDocumentInteraction( rasURI, docID,docRev ));
        interactions.add( new checkIndexPOSTInteraction( rasURI, docID,docRev ));
        interactions.add( new checkDatabaseHasDocumentInteraction( rasURI, docID,docRev ));
        interactions.add( new checkIndexPOSTInteraction( rasURI, docID,docRev ));

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        CouchdbValidator validatorUnderTest = new CouchdbValidatorImpl();
        HttpRequestFactory requestFactory = new HttpRequestFactoryImpl("Basic", "checkisvalid");

        // When..
        Throwable thrown = catchThrowable(()->validatorUnderTest.checkCouchdbDatabaseIsValid( CouchdbTestFixtures.rasUri , mockHttpClient, requestFactory));

        assertThat(thrown).isNull();
    }

    @Test
    public void TestRasStoreCreatesDBIfDBNotPresentThrowsException() throws Exception {
        String rasURI = CouchdbTestFixtures.rasUriStr;
        String docID = CouchdbTestFixtures.documentId1; 
        String docRev = CouchdbTestFixtures.documentRev1;
        List <HttpInteraction> interactions = new ArrayList<HttpInteraction>();

        //Check Welcome Screen
        interactions.add( new WelcomeInteractionOK( rasURI, docID,docRev ){
            @Override
            public MockCloseableHttpResponse getResponse() {
    
                // We expect a request to the couchdb system.
                // We will reply with a PutPostResponse
    
                Welcome welcomeBean = new Welcome();
                welcomeBean.couchdb = "Welcome";
                welcomeBean.version = "3.3.3";
    
                GalasaGson gson = new GalasaGson();
                String updateMessagePayload = gson.toJson(welcomeBean);
    
                HttpEntity entity = new MockHttpEntity(updateMessagePayload); 
    
                MockCloseableHttpResponse response = new MockCloseableHttpResponse();
    
                MockStatusLine statusLine = new MockStatusLine();
                statusLine.setStatusCode(HttpStatus.SC_OK);
                response.setStatusLine(statusLine);
                response.setEntity(entity);

                return response;
            }
        } );

        //Add bad Interaction for checking the databases are present
        interactions.add( new checkDatabasePresentInteraction( rasURI, docID,docRev )
            {
                @Override
                public MockCloseableHttpResponse getResponse() {

                MockCloseableHttpResponse response = new MockCloseableHttpResponse();
    
                MockStatusLine statusLine = new MockStatusLine();
                statusLine.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                response.setStatusLine(statusLine);

                return response;
            }
            });

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        CouchdbValidator validatorUnderTest = new CouchdbValidatorImpl();
        HttpRequestFactory requestFactory = new HttpRequestFactoryImpl("Basic", "checkisvalid");

        // When..
        Throwable thrown = catchThrowable(()->validatorUnderTest.checkCouchdbDatabaseIsValid( CouchdbTestFixtures.rasUri , mockHttpClient, requestFactory));

        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("Validation failed of database galasa_run");
    }

    @Test
    public void TestRasStoreCreatesDBIfDBNotPresentConflictResultsInExceptionAfterRetries() throws Exception {
        String rasURI = CouchdbTestFixtures.rasUriStr;
        String docID = CouchdbTestFixtures.documentId1; 
        String docRev = CouchdbTestFixtures.documentRev1;

        List <HttpInteraction> interactions = new ArrayList<HttpInteraction>();

        //Check Welcome Screen
        interactions.add( new WelcomeInteractionOK( rasURI, docID,docRev ){
            @Override
            public MockCloseableHttpResponse getResponse() {
    
                // We expect a request to the couchdb system.
                // We will reply with a PutPostResponse
    
                Welcome welcomeBean = new Welcome();
                welcomeBean.couchdb = "Welcome";
                welcomeBean.version = "3.3.3";
    
                GalasaGson gson = new GalasaGson();
                String updateMessagePayload = gson.toJson(welcomeBean);
    
                HttpEntity entity = new MockHttpEntity(updateMessagePayload); 
    
                MockCloseableHttpResponse response = new MockCloseableHttpResponse();
    
                MockStatusLine statusLine = new MockStatusLine();
                statusLine.setStatusCode(HttpStatus.SC_OK);
                response.setStatusLine(statusLine);
                response.setEntity(entity);

                return response;
            }
        } );

        // Add interaction for database check which results in not found (i.e. need to create database)
        
        //Add Interactions for checking the databases are present which result inc onflict to max out retry attempts
        interactions.add( new checkDatabasePresentNotFoundInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentConflictInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentNotFoundInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentConflictInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentNotFoundInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentConflictInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentNotFoundInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentConflictInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentNotFoundInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentConflictInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentNotFoundInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentConflictInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentNotFoundInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentConflictInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentNotFoundInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentConflictInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentNotFoundInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentConflictInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentNotFoundInteraction(rasURI, docID,docRev));
        interactions.add( new checkDatabasePresentConflictInteraction(rasURI, docID,docRev));

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        CouchdbValidator validatorUnderTest = new CouchdbValidatorImpl();
        HttpRequestFactory requestFactory = new HttpRequestFactoryImpl("Basic", "checkisvalid");

        // When..
        Throwable thrown = catchThrowable(()->validatorUnderTest.checkCouchdbDatabaseIsValid( CouchdbTestFixtures.rasUri , mockHttpClient, requestFactory));

        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("Create Database galasa_run failed on CouchDB server due to conflicts, attempted 10 times");
    }
}