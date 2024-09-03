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
import dev.galasa.auth.couchdb.internal.CouchdbUser;
import dev.galasa.extensions.common.couchdb.pojos.IdRev;
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
import dev.galasa.framework.spi.auth.IInternalAuthToken;

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

    class GetTokenDocumentInteraction<T> extends BaseHttpInteraction {

        public GetTokenDocumentInteraction(String expectedUri, int responseStatusCode, T responseObjToReturn) {
            super(expectedUri, responseStatusCode);
            setResponsePayload(responseObjToReturn);
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("GET");
        }
    }

    class DeleteTokenDocumentInteraction extends BaseHttpInteraction {

        public DeleteTokenDocumentInteraction(String expectedUri, int responseStatusCode) {
            super(expectedUri, responseStatusCode);
            setResponsePayload("");
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("DELETE");
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

        CouchdbAuthToken mockToken = new CouchdbAuthToken("token1", "dex-client", "my test token", Instant.now(), new CouchdbUser("johndoe", "dex-user-id"));
        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();
        interactions.add(new GetAllTokenDocumentsInteraction("https://my-auth-store/galasa_tokens/_all_docs", HttpStatus.SC_OK, mockAllDocsResponse));
        interactions.add(new GetTokenDocumentInteraction<CouchdbAuthToken>("https://my-auth-store/galasa_tokens/token1", HttpStatus.SC_OK, mockToken));

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        MockHttpClientFactory httpClientFactory = new MockHttpClientFactory(mockHttpClient);
        MockTimeService mockTimeService = new MockTimeService(Instant.now());

        CouchdbAuthStore authStore = new CouchdbAuthStore(authStoreUri, httpClientFactory, new HttpRequestFactoryImpl(), logFactory, new MockCouchdbValidator(), mockTimeService);

        // When...
        List<IInternalAuthToken> tokens = authStore.getTokens();

        // Then...
        assertThat(tokens).hasSize(1);

        IInternalAuthToken actualToken = tokens.get(0);
        assertThat(actualToken).usingRecursiveComparison().isEqualTo(mockToken);
    }

    @Test
    public void testGetTokensReturnsTokensByLoginIdFromCouchdbOK() throws Exception {
        // Given...
        URI authStoreUri = URI.create("couchdb:https://my-auth-store");
        MockLogFactory logFactory = new MockLogFactory();

        ViewRow tokenDoc = new ViewRow();
        tokenDoc.key = "token1";
        List<ViewRow> mockDocs = List.of(tokenDoc);

        ViewResponse mockAllDocsResponse = new ViewResponse();
        mockAllDocsResponse.rows = mockDocs;

        CouchdbAuthToken mockToken = new CouchdbAuthToken("token1", "dex-client", "my test token", Instant.now(), new CouchdbUser("johndoe", "dex-user-id"));
        CouchdbAuthToken mockToken2 = new CouchdbAuthToken("token2", "dex-client", "my test token", Instant.now(), new CouchdbUser("notJohnDoe", "dex-user-id"));
        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();
        interactions.add(new GetAllTokenDocumentsInteraction("https://my-auth-store/galasa_tokens/_design/docs/_view/loginId-view?key=johndoe", HttpStatus.SC_OK, mockAllDocsResponse));
        interactions.add(new GetTokenDocumentInteraction<CouchdbAuthToken>("https://my-auth-store/galasa_tokens/token1", HttpStatus.SC_OK, mockToken));
        interactions.add(new GetTokenDocumentInteraction<CouchdbAuthToken>("https://my-auth-store/galasa_tokens/token1", HttpStatus.SC_OK, mockToken2));

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        MockHttpClientFactory httpClientFactory = new MockHttpClientFactory(mockHttpClient);
        MockTimeService mockTimeService = new MockTimeService(Instant.now());

        CouchdbAuthStore authStore = new CouchdbAuthStore(authStoreUri, httpClientFactory, new HttpRequestFactoryImpl(), logFactory, new MockCouchdbValidator(), mockTimeService);

        // When...
        List<IInternalAuthToken> tokens = authStore.getTokensByLoginId("johndoe");

        // Then...
        assertThat(tokens).hasSize(1);

        IInternalAuthToken actualToken = tokens.get(0);
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
        authStore.storeToken("this-is-a-dex-id", "my token", new CouchdbUser("user1", "user1-id"));

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
        AuthStoreException thrown = catchThrowableOfType(() -> authStore.storeToken("this-is-a-dex-id", "my token", new CouchdbUser("user1", "user1-id")), AuthStoreException.class);

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("GAL6102E", "Failed to store auth token in the CouchDB tokens database");
    }

    @Test
    public void testDeleteTokenSendsRequestToDeleteTokenDocumentOK() throws Exception {
        // Given...
        URI authStoreUri = URI.create("couchdb:https://my-auth-store");
        MockLogFactory logFactory = new MockLogFactory();

        String tokenIdToDelete = "my-old-token";

        IdRev mockIdRev = new IdRev();
        mockIdRev._rev = "this-is-a-revision";
        
        String expectedGetRequestUrl = "https://my-auth-store/galasa_tokens/" + tokenIdToDelete;
        String expectedDeleteRequestUrl = "https://my-auth-store/galasa_tokens/" + tokenIdToDelete + "?rev=" + mockIdRev._rev;

        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();
        interactions.add(new GetTokenDocumentInteraction<IdRev>(expectedGetRequestUrl, HttpStatus.SC_OK, mockIdRev));
        interactions.add(new DeleteTokenDocumentInteraction(expectedDeleteRequestUrl, HttpStatus.SC_OK));

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        MockHttpClientFactory httpClientFactory = new MockHttpClientFactory(mockHttpClient);
        MockTimeService mockTimeService = new MockTimeService(Instant.now());

        CouchdbAuthStore authStore = new CouchdbAuthStore(authStoreUri, httpClientFactory, new HttpRequestFactoryImpl(), logFactory, new MockCouchdbValidator(), mockTimeService);

        // When...
        authStore.deleteToken(tokenIdToDelete);

        // Then the assertions made in the document interactions shouldn't have failed.
    }

    @Test
    public void testDeleteTokenWithAcceptedRequestToDeleteTokenDocumentDoesNotError() throws Exception {
        // Given...
        URI authStoreUri = URI.create("couchdb:https://my-auth-store");
        MockLogFactory logFactory = new MockLogFactory();

        String tokenIdToDelete = "my-old-token";

        IdRev mockIdRev = new IdRev();
        mockIdRev._rev = "this-is-a-revision";
        
        String expectedGetRequestUrl = "https://my-auth-store/galasa_tokens/" + tokenIdToDelete;
        String expectedDeleteRequestUrl = "https://my-auth-store/galasa_tokens/" + tokenIdToDelete + "?rev=" + mockIdRev._rev;

        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();
        interactions.add(new GetTokenDocumentInteraction<IdRev>(expectedGetRequestUrl, HttpStatus.SC_OK, mockIdRev));

        // The DELETE request may return a 202 Accepted, which shouldn't be a problem for us
        interactions.add(new DeleteTokenDocumentInteraction(expectedDeleteRequestUrl, HttpStatus.SC_ACCEPTED));

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        MockHttpClientFactory httpClientFactory = new MockHttpClientFactory(mockHttpClient);
        MockTimeService mockTimeService = new MockTimeService(Instant.now());

        CouchdbAuthStore authStore = new CouchdbAuthStore(authStoreUri, httpClientFactory, new HttpRequestFactoryImpl(), logFactory, new MockCouchdbValidator(), mockTimeService);

        // When...
        authStore.deleteToken(tokenIdToDelete);

        // Then the assertions made in the document interactions shouldn't have failed.
    }

    @Test
    public void testDeleteTokenWithFailingRequestToDeleteTokenDocumentThrowsError() throws Exception {
        // Given...
        URI authStoreUri = URI.create("couchdb:https://my-auth-store");
        MockLogFactory logFactory = new MockLogFactory();

        String tokenIdToDelete = "my-old-token";

        IdRev mockIdRev = new IdRev();
        mockIdRev._rev = "this-is-a-revision";
        
        String expectedGetRequestUrl = "https://my-auth-store/galasa_tokens/" + tokenIdToDelete;
        String expectedDeleteRequestUrl = "https://my-auth-store/galasa_tokens/" + tokenIdToDelete + "?rev=" + mockIdRev._rev;

        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();
        interactions.add(new GetTokenDocumentInteraction<IdRev>(expectedGetRequestUrl, HttpStatus.SC_OK, mockIdRev));

        // Simulate an internal server error
        interactions.add(new DeleteTokenDocumentInteraction(expectedDeleteRequestUrl, HttpStatus.SC_INTERNAL_SERVER_ERROR));

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        MockHttpClientFactory httpClientFactory = new MockHttpClientFactory(mockHttpClient);
        MockTimeService mockTimeService = new MockTimeService(Instant.now());

        CouchdbAuthStore authStore = new CouchdbAuthStore(authStoreUri, httpClientFactory, new HttpRequestFactoryImpl(), logFactory, new MockCouchdbValidator(), mockTimeService);

        // When...
        AuthStoreException thrown = catchThrowableOfType(() -> {
            authStore.deleteToken(tokenIdToDelete);
        }, AuthStoreException.class);

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("GAL6104E", "Failed to delete auth token from the CouchDB tokens database");
        assertThat(thrown.getMessage()).contains("GAL6007E", "Expected status code(s) [200, 202] but received 500");
    }

    @Test
    public void testDeleteTokenWithFailingRequestToGetTokenDocumentThrowsError() throws Exception {
        // Given...
        URI authStoreUri = URI.create("couchdb:https://my-auth-store");
        MockLogFactory logFactory = new MockLogFactory();

        String tokenIdToDelete = "my-old-token";

        IdRev mockIdRev = new IdRev();
        mockIdRev._rev = "this-is-a-revision";
        
        String expectedGetRequestUrl = "https://my-auth-store/galasa_tokens/" + tokenIdToDelete;

        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();

        // Simulate an internal server error
        interactions.add(new GetTokenDocumentInteraction<IdRev>(expectedGetRequestUrl, HttpStatus.SC_INTERNAL_SERVER_ERROR, mockIdRev));

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        MockHttpClientFactory httpClientFactory = new MockHttpClientFactory(mockHttpClient);
        MockTimeService mockTimeService = new MockTimeService(Instant.now());

        CouchdbAuthStore authStore = new CouchdbAuthStore(authStoreUri, httpClientFactory, new HttpRequestFactoryImpl(), logFactory, new MockCouchdbValidator(), mockTimeService);

        // When...
        AuthStoreException thrown = catchThrowableOfType(() -> {
            authStore.deleteToken(tokenIdToDelete);
        }, AuthStoreException.class);

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("GAL6104E", "Failed to delete auth token from the CouchDB tokens database");
    }

    @Test
    public void testDeleteTokenWithBadGetTokenDocumentResponseBodyThrowsError() throws Exception {
        // Given...
        URI authStoreUri = URI.create("couchdb:https://my-auth-store");
        MockLogFactory logFactory = new MockLogFactory();

        String tokenIdToDelete = "my-old-token";
        
        String expectedGetRequestUrl = "https://my-auth-store/galasa_tokens/" + tokenIdToDelete;

        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();

        // Simulate an internal server error
        interactions.add(new GetTokenDocumentInteraction<IdRev>(expectedGetRequestUrl, HttpStatus.SC_OK, null));

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        MockHttpClientFactory httpClientFactory = new MockHttpClientFactory(mockHttpClient);
        MockTimeService mockTimeService = new MockTimeService(Instant.now());

        CouchdbAuthStore authStore = new CouchdbAuthStore(authStoreUri, httpClientFactory, new HttpRequestFactoryImpl(), logFactory, new MockCouchdbValidator(), mockTimeService);

        // When...
        AuthStoreException thrown = catchThrowableOfType(() -> {
            authStore.deleteToken(tokenIdToDelete);
        }, AuthStoreException.class);

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("GAL6104E", "Failed to delete auth token from the CouchDB tokens database");
        assertThat(thrown.getMessage()).contains("GAL6011E", "Failed to get document with ID 'my-old-token' from the 'galasa_tokens' database");
    }
}
