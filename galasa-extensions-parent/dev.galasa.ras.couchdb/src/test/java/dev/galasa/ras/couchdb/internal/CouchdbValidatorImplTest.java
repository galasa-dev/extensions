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
import dev.galasa.ras.couchdb.internal.mocks.*;
import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures.BaseHttpInteraction;
import dev.galasa.ras.couchdb.internal.pojos.Welcome;
import dev.galasa.ras.couchdb.internal.dependencies.impl.HttpRequestFactory;;

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
        MockEnvironment environment = new MockEnvironment();
        environment.setenv("GALASA_RAS_TOKEN", "checkisvalid");
        HttpRequestFactory requestFactory = new HttpRequestFactory(environment);

        // When..
        Throwable thrown = catchThrowable(()-> validatorUnderTest.checkCouchdbDatabaseIsValid( CouchdbTestFixtures.rasUri , mockHttpClient, requestFactory));

        // Then..
        assertThat(thrown).isNotNull();
        assertThat(thrown).as("exception caught is of type "+thrown.getClass().toString()).isInstanceOf(CouchdbRasException.class);
    }
}