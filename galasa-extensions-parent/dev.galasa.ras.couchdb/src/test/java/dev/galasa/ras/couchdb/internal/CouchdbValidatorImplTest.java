/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import org.junit.Rule;
import org.junit.rules.TestName;

public class CouchdbValidatorImplTest {
    
    @Rule
    public TestName testName = new TestName();


    // @Test
    // public void TestRasStoreCreateBlowsUpIfCouchDBDoesntReturnWelcomeString() throws Exception {


    //     // Given...
    //     URI rasURI = URI.create("http://my.uri");

    //     StatusLine statusLine = new MockStatusLine();

    //     Welcome welcomeBean = new Welcome();
    //     welcomeBean.couchdb = "dummy-edition";
    //     welcomeBean.version = "2.3.1";

    //     Gson gson = GalasaGsonBuilder.build();
    //     String welcomeToCouchDBMessage = gson.toJson(welcomeBean);

    //     HttpEntity entity = new MockHttpEntity(welcomeToCouchDBMessage);

    //     MockCloseableHttpResponse response = new MockCloseableHttpResponse();
    //     response.setStatusLine(statusLine);
    //     response.setEntity(entity);

    //     MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(response);

    //     CouchdbValidator validatorUnderTest = new CouchdbValidatorImpl();

    //     // When..
    //     Throwable thrown = catchThrowable(()-> validatorUnderTest.checkCouchdbDatabaseIsValid( rasURI , mockHttpClient ));

    //     // Then..
    //     assertThat(thrown).isInstanceOf(CouchdbRasException.class);
    // }
}