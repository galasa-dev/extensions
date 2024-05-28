/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
import dev.galasa.extensions.mocks.MockHttpClientFactory;
import dev.galasa.extensions.mocks.MockLogFactory;
import dev.galasa.extensions.mocks.MockTimeService;
import dev.galasa.extensions.mocks.couchdb.MockCouchdbValidator;
import dev.galasa.framework.spi.auth.AuthStoreException;
import dev.galasa.framework.spi.auth.IAuthToken;
import dev.galasa.framework.spi.auth.User;

public class TestCouchdbAuthStore {

    class GetAllTokenDocumentsInteraction extends BaseHttpInteraction {

        public GetAllTokenDocumentsInteraction(String expectedUri, int responseStatusCode, ViewResponse tokenDocsToReturn) {
            super(expectedUri, responseStatusCode);
            setResponsePayload(tokenDocsToReturn);
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("GET");
        }
    }

    class GetTokenDocumentInteraction extends BaseHttpInteraction {

        public GetTokenDocumentInteraction(String expectedUri, int responseStatusCode, CouchdbAuthToken tokenToReturn) {
            super(expectedUri, responseStatusCode);
            setResponsePayload(tokenToReturn);
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("GET");
        }
    }

    class CreateTokenDocInteraction extends BaseHttpInteraction {

        public CreateTokenDocInteraction(String expectedUri, int responseStatusCode) {
            super(expectedUri, responseStatusCode);

            PutPostResponse responseTransportBean = new PutPostResponse();
            responseTransportBean.id = "id";
            responseTransportBean.ok = true;
            responseTransportBean.rev = "rev";
            setResponsePayload(responseTransportBean);
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("POST");
        }
    }

    @Test
    public void testGetTokensReturnsTokensWithFailingRequestReturnsError() throws Exception {
        // Given...
        URI authStoreUri = URI.create("couchdb:https://my-auth-store");
        MockLogFactory logFactory = new MockLogFactory();

        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();
        interactions.add(new GetAllTokenDocumentsInteraction("https://my-auth-store/galasa_tokens/_all_docs", HttpStatus.SC_INTERNAL_SERVER_ERROR, null));

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        MockHttpClientFactory httpClientFactory = new MockHttpClientFactory(mockHttpClient);
        MockTimeService mockTimeService = new MockTimeService(Instant.now());

        CouchdbAuthStore authStore = new CouchdbAuthStore(authStoreUri, httpClientFactory, new HttpRequestFactoryImpl(), logFactory, new MockCouchdbValidator(), mockTimeService);

        // When...
        AuthStoreException thrown = catchThrowableOfType(() -> authStore.getTokens(), AuthStoreException.class);

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("GAL6101E", "Failed to get auth tokens from the CouchDB auth store");
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
        interactions.add(new GetAllTokenDocumentsInteraction("https://my-auth-store/galasa_tokens/_all_docs", HttpStatus.SC_OK, mockAllDocsResponse));
        interactions.add(new GetTokenDocumentInteraction("https://my-auth-store/galasa_tokens/token1", HttpStatus.SC_OK, mockToken));

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
        interactions.add(new CreateTokenDocInteraction("https://my-auth-store/galasa_tokens", HttpStatus.SC_CREATED));

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        MockHttpClientFactory httpClientFactory = new MockHttpClientFactory(mockHttpClient);
        MockTimeService mockTimeService = new MockTimeService(Instant.now());

        CouchdbAuthStore authStore = new CouchdbAuthStore(authStoreUri, httpClientFactory, new HttpRequestFactoryImpl(), logFactory, new MockCouchdbValidator(), mockTimeService);

        // When...
        authStore.storeToken("this-is-a-dex-id", "my token", new User("user1"));

        // Then the assertions made in the create token document interaction shouldn't have failed.
    }

    @Test
    public void testStoreTokenWithFailingRequestToCreateTokenDocumentReturnsError() throws Exception {
        // Given...
        URI authStoreUri = URI.create("couchdb:https://my-auth-store");
        MockLogFactory logFactory = new MockLogFactory();

        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();
        interactions.add(new CreateTokenDocInteraction("https://my-auth-store/galasa_tokens", HttpStatus.SC_INTERNAL_SERVER_ERROR));

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        MockHttpClientFactory httpClientFactory = new MockHttpClientFactory(mockHttpClient);
        MockTimeService mockTimeService = new MockTimeService(Instant.now());

        CouchdbAuthStore authStore = new CouchdbAuthStore(authStoreUri, httpClientFactory, new HttpRequestFactoryImpl(), logFactory, new MockCouchdbValidator(), mockTimeService);

        // When...
        AuthStoreException thrown = catchThrowableOfType(() -> authStore.storeToken("this-is-a-dex-id", "my token", new User("user1")), AuthStoreException.class);

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("GAL6102E", "Failed to store auth token in the CouchDB tokens database");
    }
}
