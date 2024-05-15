/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.junit.Test;

import dev.galasa.auth.couchdb.internal.CouchdbAuthStore;
import dev.galasa.auth.couchdb.internal.CouchdbAuthToken;
import dev.galasa.extensions.common.couchdb.pojos.PutPostResponse;
import dev.galasa.extensions.common.couchdb.pojos.ViewResponse;
import dev.galasa.extensions.common.couchdb.pojos.ViewRow;
import dev.galasa.extensions.common.impl.HttpRequestFactoryImpl;
import dev.galasa.extensions.mocks.BaseHttpInteraction;
import dev.galasa.extensions.mocks.HttpInteraction;
import dev.galasa.extensions.mocks.MockCloseableHttpClient;
import dev.galasa.extensions.mocks.MockCloseableHttpResponse;
import dev.galasa.extensions.mocks.MockHttpClientFactory;
import dev.galasa.extensions.mocks.MockHttpEntity;
import dev.galasa.extensions.mocks.MockLogFactory;
import dev.galasa.extensions.mocks.MockStatusLine;
import dev.galasa.extensions.mocks.MockTimeService;
import dev.galasa.extensions.mocks.couchdb.MockCouchdbValidator;
import dev.galasa.framework.spi.auth.IAuthToken;
import dev.galasa.framework.spi.auth.User;
import dev.galasa.framework.spi.utils.GalasaGson;

public class TestCouchdbAuthStore {

    class GetAllTokenDocumentsInteraction extends BaseHttpInteraction {

        private ViewResponse tokenDocsToReturn;

        public GetAllTokenDocumentsInteraction(String expectedUri, ViewResponse tokenDocsToReturn) {
            super(expectedUri, null);
            this.tokenDocsToReturn = tokenDocsToReturn;
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("GET");
        }

        @Override
        public MockCloseableHttpResponse getResponse() {

            GalasaGson gson = new GalasaGson();
            String msgPayload = gson.toJson(this.tokenDocsToReturn);

            HttpEntity entity = new MockHttpEntity(msgPayload);

            MockCloseableHttpResponse response = new MockCloseableHttpResponse();

            MockStatusLine statusLine = new MockStatusLine();
            statusLine.setStatusCode(HttpStatus.SC_OK);
            response.setStatusLine(statusLine);
            response.setEntity(entity);

            return response;
        }
    }

    class GetTokenDocumentInteraction extends BaseHttpInteraction {

        private CouchdbAuthToken tokenToReturn;

        public GetTokenDocumentInteraction(String expectedUri, CouchdbAuthToken tokenToReturn) {
            super(expectedUri, null);
            this.tokenToReturn = tokenToReturn;
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("GET");
        }

        @Override
        public MockCloseableHttpResponse getResponse() {

            GalasaGson gson = new GalasaGson();
            String msgPayload = gson.toJson(this.tokenToReturn);

            HttpEntity entity = new MockHttpEntity(msgPayload);

            MockCloseableHttpResponse response = new MockCloseableHttpResponse();

            MockStatusLine statusLine = new MockStatusLine();
            statusLine.setStatusCode(HttpStatus.SC_OK);
            response.setStatusLine(statusLine);
            response.setEntity(entity);

            return response;
        }
    }

    class CreateTokenDocInteractionOK extends BaseHttpInteraction {

        public CreateTokenDocInteractionOK(String expectedUri, String documentId) {
            super(expectedUri, documentId);
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("POST");
        }

        @Override
        public MockCloseableHttpResponse getResponse() {
            PutPostResponse responseTransportBean = new PutPostResponse();
            responseTransportBean.id = getReturnedDocument();
            responseTransportBean.ok = true;
            responseTransportBean.rev = getReturnedDocument();

            GalasaGson gson = new GalasaGson();
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
    public void testGetTokensReturnsTokensFromCouchdbOK() throws Exception {
        // Given...
        URI authStoreUri = URI.create("couchdb:https://my-auth-store");
        MockLogFactory logFactory = new MockLogFactory();

        ViewRow tokenDoc = new ViewRow();
        tokenDoc.key = "token1";
        List<ViewRow> mockDocs = List.of(tokenDoc);

        ViewResponse mockAllDocsResponse = new ViewResponse();
        mockAllDocsResponse.rows = mockDocs;

        CouchdbAuthToken mockToken = new CouchdbAuthToken("token1", "dex-client", "my test token", Instant.now(), new User("johndoe"));
        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();
        interactions.add(new GetAllTokenDocumentsInteraction("https://my-auth-store/galasa_tokens/_all_docs", mockAllDocsResponse));
        interactions.add(new GetTokenDocumentInteraction("https://my-auth-store/galasa_tokens/token1", mockToken));

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        MockHttpClientFactory httpClientFactory = new MockHttpClientFactory(mockHttpClient);
        MockTimeService mockTimeService = new MockTimeService(Instant.now());

        CouchdbAuthStore authStore = new CouchdbAuthStore(authStoreUri, httpClientFactory, new HttpRequestFactoryImpl(), logFactory, new MockCouchdbValidator(), mockTimeService);

        // When...
        List<IAuthToken> tokens = authStore.getTokens();

        // Then...
        assertThat(tokens).hasSize(1);

        IAuthToken actualToken = tokens.get(0);
        assertThat(actualToken).usingRecursiveComparison().isEqualTo(mockToken);
    }

    @Test
    public void testStoreTokenSendsRequestToCreateTokenDocumentOK() throws Exception {
        // Given...
        URI authStoreUri = URI.create("couchdb:https://my-auth-store");
        MockLogFactory logFactory = new MockLogFactory();

        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();
        interactions.add(new CreateTokenDocInteractionOK("https://my-auth-store/galasa_tokens", "token-document-1"));

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);
        
        MockHttpClientFactory httpClientFactory = new MockHttpClientFactory(mockHttpClient);
        MockTimeService mockTimeService = new MockTimeService(Instant.now());

        CouchdbAuthStore authStore = new CouchdbAuthStore(authStoreUri, httpClientFactory, new HttpRequestFactoryImpl(), logFactory, new MockCouchdbValidator(), mockTimeService);

        // When...
        authStore.storeToken("this-is-a-dex-id", "my token", new User("user1"));

        // Then the assertions made in the create token document interaction shouldn't have failed.
    }
}
