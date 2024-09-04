/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import dev.galasa.auth.couchdb.internal.CouchdbAuthStore;
import dev.galasa.auth.couchdb.internal.CouchdbAuthStoreValidator;
import dev.galasa.auth.couchdb.internal.beans.*;
import dev.galasa.extensions.common.couchdb.CouchdbBaseValidator;
import dev.galasa.extensions.common.couchdb.CouchdbException;
import dev.galasa.extensions.common.couchdb.pojos.PutPostResponse;
import dev.galasa.extensions.common.couchdb.pojos.Welcome;
import dev.galasa.extensions.common.impl.HttpRequestFactoryImpl;
import dev.galasa.extensions.mocks.BaseHttpInteraction;
import dev.galasa.extensions.mocks.HttpInteraction;
import dev.galasa.extensions.mocks.MockCloseableHttpClient;

public class TestCouchdbAuthStoreValidator {

    class CreateDatabaseInteraction extends BaseHttpInteraction {

        public CreateDatabaseInteraction(String expectedUri, int responseStatusCode) {
            super(expectedUri, responseStatusCode);
            
            PutPostResponse responseTransportBean = new PutPostResponse();
            responseTransportBean.id = "id";
            responseTransportBean.ok = String.valueOf(responseStatusCode).startsWith("2");
            responseTransportBean.rev = "rev";
            setResponsePayload(responseTransportBean);
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("PUT");
        }
    }

    class GetCouchdbWelcomeInteraction extends BaseHttpInteraction {

        public GetCouchdbWelcomeInteraction(String expectedUri, Welcome welcomeMessage) {
            super(expectedUri, HttpStatus.SC_OK);
            setResponsePayload(welcomeMessage);
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("GET");
        }
    }

    class GetTokensDatabaseInteraction extends BaseHttpInteraction {

        public GetTokensDatabaseInteraction(String expectedUri, int responseStatusCode) {
            super(expectedUri, responseStatusCode);
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("HEAD");
        }
    }

    class GetTokensDatabaseDesignInteraction extends BaseHttpInteraction {
        public GetTokensDatabaseDesignInteraction(String expectedUri, Object returnedDocument) {
            this(expectedUri, returnedDocument, HttpStatus.SC_OK);
        }

        public GetTokensDatabaseDesignInteraction(String expectedUri, Object returnedDocument, int expectedResponseCode) {
            super(expectedUri, returnedDocument, expectedResponseCode);
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("GET");
        }
    }

    class UpdateTokensDatabaseDesignInteraction extends BaseHttpInteraction {
        public UpdateTokensDatabaseDesignInteraction(String expectedUri, String returnedDocument, int expectedResponseCode) {
            super(expectedUri, returnedDocument, expectedResponseCode);
            setResponsePayload(returnedDocument);
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("PUT");
        }
    }



//   "_id": "_design/docs",
//   "_rev": "3-9e69612124f138c029ab40c9c9072deb",
//   "views": {
//     "loginId-view": {
//       "map": "function (doc) {\n  if (doc.owner && doc.owner.loginId) {\n    emit(doc.owner.loginId, doc);\n  }\n}"
//     }
//   },
//   "language": "javascript"
// }    


    @Test
    public void testCheckCouchdbDatabaseIsValidWithValidDatabaseIsOK() throws Exception {
        // Given...
        String couchdbUriStr = "https://my-couchdb-server";
        URI couchdbUri = URI.create(couchdbUriStr);
        CouchdbAuthStoreValidator validator = new CouchdbAuthStoreValidator();

        Welcome welcomeMessage = new Welcome();
        welcomeMessage.couchdb = "Welcome";
        welcomeMessage.version = CouchdbBaseValidator.COUCHDB_MIN_VERSION;

        List<HttpInteraction> interactions = new ArrayList<>();
        interactions.add(new GetCouchdbWelcomeInteraction(couchdbUriStr, welcomeMessage));
        interactions.add(new GetTokensDatabaseInteraction(couchdbUriStr + "/" + CouchdbAuthStore.TOKENS_DATABASE_NAME, HttpStatus.SC_OK));


        // We are expecting this to ne returned from our mock couchdb server to the code:
        //   "_id": "_design/docs",
        //   "_rev": "3-9e69612124f138c029ab40c9c9072deb",
        //   "views": {
        //     "loginId-view": {
        //       "map": "function (doc) {\n  if (doc.owner && doc.owner.loginId) {\n    emit(doc.owner.loginId, doc);\n  }\n}"
        //     }
        //   },
        //   "language": "javascript"
        // }
        TokenDBLoginView view = new TokenDBLoginView();
        view.map = "function (doc) {\n  if (doc.owner && doc.owner.loginId) {\n    emit(doc.owner.loginId, doc);\n  }\n}";
        TokenDBViews views = new TokenDBViews();
        views.loginIdView = view;
        TokensDBNameViewDesign designDocToPassBack = new TokensDBNameViewDesign();
        designDocToPassBack.language = "javascript";


        String tokensDesignDocUrl = couchdbUriStr + "/" + CouchdbAuthStore.TOKENS_DATABASE_NAME + "/_design/docs";
        interactions.add(new GetTokensDatabaseDesignInteraction(tokensDesignDocUrl, designDocToPassBack));


        interactions.add(new UpdateTokensDatabaseDesignInteraction(tokensDesignDocUrl, "", HttpStatus.SC_CREATED));
        CloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        // When...
        validator.checkCouchdbDatabaseIsValid(couchdbUri, mockHttpClient, new HttpRequestFactoryImpl());
    }

    @Test
    public void testCheckCouchdbDatabaseIsValidWithFailingDatabaseCreationReturnsError() throws Exception {
        // Given...
        String couchdbUriStr = "https://my-couchdb-server";
        URI couchdbUri = URI.create(couchdbUriStr);
        CouchdbAuthStoreValidator validator = new CouchdbAuthStoreValidator();

        Welcome welcomeMessage = new Welcome();
        welcomeMessage.couchdb = "Welcome";
        welcomeMessage.version = CouchdbBaseValidator.COUCHDB_MIN_VERSION;

        String tokensDatabaseName = CouchdbAuthStore.TOKENS_DATABASE_NAME;
        List<HttpInteraction> interactions = new ArrayList<>();
        interactions.add(new GetCouchdbWelcomeInteraction(couchdbUriStr, welcomeMessage));
        interactions.add(new GetTokensDatabaseInteraction(couchdbUriStr + "/" + tokensDatabaseName, HttpStatus.SC_NOT_FOUND));
        interactions.add(new CreateDatabaseInteraction(couchdbUriStr + "/" + tokensDatabaseName, HttpStatus.SC_INTERNAL_SERVER_ERROR));
        CloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        // When...
        CouchdbException thrown = catchThrowableOfType(
            () -> validator.checkCouchdbDatabaseIsValid(couchdbUri, mockHttpClient, new HttpRequestFactoryImpl()),
            CouchdbException.class
        );

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("GAL6004E", "Failed to create CouchDB database '" + tokensDatabaseName + "'", "Status code 500 from CouchDB server is not 201");
    }

    @Test
    public void testCheckCouchdbDatabaseIsValidWithSuccessfulDatabaseCreationIsOK() throws Exception {
        // Given...
        String couchdbUriStr = "https://my-couchdb-server";
        URI couchdbUri = URI.create(couchdbUriStr);
        CouchdbAuthStoreValidator validator = new CouchdbAuthStoreValidator();

        Welcome welcomeMessage = new Welcome();
        welcomeMessage.couchdb = "Welcome";
        welcomeMessage.version = CouchdbBaseValidator.COUCHDB_MIN_VERSION;

        String tokensDatabaseName = CouchdbAuthStore.TOKENS_DATABASE_NAME;
        List<HttpInteraction> interactions = new ArrayList<>();
        interactions.add(new GetCouchdbWelcomeInteraction(couchdbUriStr, welcomeMessage));
        interactions.add(new GetTokensDatabaseInteraction(couchdbUriStr + "/" + tokensDatabaseName, HttpStatus.SC_NOT_FOUND));
        interactions.add(new CreateDatabaseInteraction(couchdbUriStr + "/" + tokensDatabaseName, HttpStatus.SC_CREATED));

        TokenDBLoginView view = new TokenDBLoginView();
        view.map = "function (doc) {\n  if (doc.owner && doc.owner.loginId) {\n    emit(doc.owner.loginId, doc);\n  }\n}";
        TokenDBViews views = new TokenDBViews();
        views.loginIdView = view;
        TokensDBNameViewDesign designDocToPassBack = new TokensDBNameViewDesign();
        designDocToPassBack.language = "javascript";


        String tokensDesignDocUrl = couchdbUriStr + "/" + tokensDatabaseName + "/_design/docs";
        interactions.add(new GetTokensDatabaseDesignInteraction(tokensDesignDocUrl, designDocToPassBack));


        interactions.add(new UpdateTokensDatabaseDesignInteraction(tokensDesignDocUrl, "",HttpStatus.SC_CREATED));
        CloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        // When...
        validator.checkCouchdbDatabaseIsValid(couchdbUri, mockHttpClient, new HttpRequestFactoryImpl());

        // Then...
        // The validation should have passed, so no errors should have been thrown
    }

    @Test
    public void testCheckCouchdbDatabaseIsValidWithNewerCouchdbVersionIsOK() throws Exception {
        // Given...
        String couchdbUriStr = "https://my-couchdb-server";
        URI couchdbUri = URI.create(couchdbUriStr);
        CouchdbAuthStoreValidator validator = new CouchdbAuthStoreValidator();

        String[] versionParts = CouchdbBaseValidator.COUCHDB_MIN_VERSION.split("\\.");
        int majorVersion = Integer.parseInt(versionParts[0]);
        int minorVersion = Integer.parseInt(versionParts[1]);
        int patchVersion = Integer.parseInt(versionParts[2]);

        Welcome welcomeMessage = new Welcome();
        welcomeMessage.couchdb = "Welcome";
        welcomeMessage.version = (majorVersion + 10) + "." + minorVersion + "." + patchVersion;

        String tokensDatabaseName = CouchdbAuthStore.TOKENS_DATABASE_NAME;
        List<HttpInteraction> interactions = new ArrayList<>();
        interactions.add(new GetCouchdbWelcomeInteraction(couchdbUriStr, welcomeMessage));
        interactions.add(new GetTokensDatabaseInteraction(couchdbUriStr + "/" + tokensDatabaseName, HttpStatus.SC_INTERNAL_SERVER_ERROR));
        CloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        // When...
        CouchdbException thrown = catchThrowableOfType(
            () -> validator.checkCouchdbDatabaseIsValid(couchdbUri, mockHttpClient, new HttpRequestFactoryImpl()),
            CouchdbException.class
        );

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("GAL6003E", "Failed to determine whether the '" + tokensDatabaseName + "' database exists");
    }

    @Test
    public void testCheckCouchdbDatabaseIsValidWithInvalidWelcomeMessageThrowsError() throws Exception {
        // Given...
        String couchdbUriStr = "https://my-couchdb-server";
        URI couchdbUri = URI.create(couchdbUriStr);
        CouchdbAuthStoreValidator validator = new CouchdbAuthStoreValidator();

        Welcome welcomeMessage = new Welcome();
        welcomeMessage.couchdb = "not welcome";
        welcomeMessage.version = CouchdbBaseValidator.COUCHDB_MIN_VERSION;

        List<HttpInteraction> interactions = new ArrayList<>();
        interactions.add(new GetCouchdbWelcomeInteraction(couchdbUriStr, welcomeMessage));
        interactions.add(new GetTokensDatabaseInteraction(couchdbUriStr + "/" + CouchdbAuthStore.TOKENS_DATABASE_NAME, HttpStatus.SC_OK));
        CloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        // When...
        CouchdbException thrown = catchThrowableOfType(
            () -> validator.checkCouchdbDatabaseIsValid(couchdbUri, mockHttpClient, new HttpRequestFactoryImpl()),
            CouchdbException.class
        );

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("GAL6001E", "Invalid CouchDB Welcome message");
    }

    @Test
    public void testCheckCouchdbDatabaseIsValidWithMajorVersionMismatchThrowsError() throws Exception {
        // Given...
        String couchdbUriStr = "https://my-couchdb-server";
        URI couchdbUri = URI.create(couchdbUriStr);
        CouchdbAuthStoreValidator validator = new CouchdbAuthStoreValidator();

        Welcome welcomeMessage = new Welcome();
        welcomeMessage.couchdb = "Welcome";
        welcomeMessage.version = "0.1.2";

        List<HttpInteraction> interactions = new ArrayList<>();
        interactions.add(new GetCouchdbWelcomeInteraction(couchdbUriStr, welcomeMessage));
        interactions.add(new GetTokensDatabaseInteraction(couchdbUriStr + "/" + CouchdbAuthStore.TOKENS_DATABASE_NAME, HttpStatus.SC_OK));
        CloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        // When...
        CouchdbException thrown = catchThrowableOfType(
            () -> validator.checkCouchdbDatabaseIsValid(couchdbUri, mockHttpClient, new HttpRequestFactoryImpl()),
            CouchdbException.class
        );

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("GAL6005E", "Expected version '" + CouchdbBaseValidator.COUCHDB_MIN_VERSION + "' or above");
    }

    @Test
    public void testCheckCouchdbDatabaseIsValidWithInvalidVersionThrowsError() throws Exception {
        // Given...
        String couchdbUriStr = "https://my-couchdb-server";
        URI couchdbUri = URI.create(couchdbUriStr);
        CouchdbAuthStoreValidator validator = new CouchdbAuthStoreValidator();

        Welcome welcomeMessage = new Welcome();
        welcomeMessage.couchdb = "Welcome";
        welcomeMessage.version = "notaversion";

        List<HttpInteraction> interactions = new ArrayList<>();
        interactions.add(new GetCouchdbWelcomeInteraction(couchdbUriStr, welcomeMessage));
        interactions.add(new GetTokensDatabaseInteraction(couchdbUriStr + "/" + CouchdbAuthStore.TOKENS_DATABASE_NAME, HttpStatus.SC_OK));
        CloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        // When...
        CouchdbException thrown = catchThrowableOfType(
            () -> validator.checkCouchdbDatabaseIsValid(couchdbUri, mockHttpClient, new HttpRequestFactoryImpl()),
            CouchdbException.class
        );

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("GAL6010E", "Invalid CouchDB server version format detected");
    }

    @Test
    public void testCheckCouchdbDatabaseIsValidWithMinorVersionMismatchThrowsError() throws Exception {
        // Given...
        String couchdbUriStr = "https://my-couchdb-server";
        URI couchdbUri = URI.create(couchdbUriStr);
        CouchdbAuthStoreValidator validator = new CouchdbAuthStoreValidator();

        String majorVersion = CouchdbBaseValidator.COUCHDB_MIN_VERSION.split("\\.")[0];

        Welcome welcomeMessage = new Welcome();
        welcomeMessage.couchdb = "Welcome";
        welcomeMessage.version = majorVersion + ".0.0";

        List<HttpInteraction> interactions = new ArrayList<>();
        interactions.add(new GetCouchdbWelcomeInteraction(couchdbUriStr, welcomeMessage));
        interactions.add(new GetTokensDatabaseInteraction(couchdbUriStr + "/" + CouchdbAuthStore.TOKENS_DATABASE_NAME, HttpStatus.SC_OK));
        CloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        // When...
        CouchdbException thrown = catchThrowableOfType(
            () -> validator.checkCouchdbDatabaseIsValid(couchdbUri, mockHttpClient, new HttpRequestFactoryImpl()),
            CouchdbException.class
        );

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("GAL6005E", "Expected version '" + CouchdbBaseValidator.COUCHDB_MIN_VERSION + "' or above");
    }

    @Test
    public void testCheckCouchdbDatabaseIsValidWithPatchVersionMismatchThrowsError() throws Exception {
        // Given...
        String couchdbUriStr = "https://my-couchdb-server";
        URI couchdbUri = URI.create(couchdbUriStr);
        CouchdbAuthStoreValidator validator = new CouchdbAuthStoreValidator();

        String[] minVersionParts = CouchdbBaseValidator.COUCHDB_MIN_VERSION.split("\\.");

        Welcome welcomeMessage = new Welcome();
        welcomeMessage.couchdb = "Welcome";
        welcomeMessage.version = String.format("%s.%s.%s", minVersionParts[0], minVersionParts[1], 0);

        List<HttpInteraction> interactions = new ArrayList<>();
        interactions.add(new GetCouchdbWelcomeInteraction(couchdbUriStr, welcomeMessage));
        interactions.add(new GetTokensDatabaseInteraction(couchdbUriStr + "/" + CouchdbAuthStore.TOKENS_DATABASE_NAME, HttpStatus.SC_OK));
        CloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        // When...
        CouchdbException thrown = catchThrowableOfType(
            () -> validator.checkCouchdbDatabaseIsValid(couchdbUri, mockHttpClient, new HttpRequestFactoryImpl()),
            CouchdbException.class
        );

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("GAL6005E", "Expected version '" + CouchdbBaseValidator.COUCHDB_MIN_VERSION + "' or above");
    }

    @Test
    public void testCheckCouchdbDatabaseIsValidWithFailedDesignDocResponseThrowsError() throws Exception {
        // Given...
        String couchdbUriStr = "https://my-couchdb-server";
        URI couchdbUri = URI.create(couchdbUriStr);
        CouchdbAuthStoreValidator validator = new CouchdbAuthStoreValidator();

        Welcome welcomeMessage = new Welcome();
        welcomeMessage.couchdb = "Welcome";
        welcomeMessage.version = CouchdbBaseValidator.COUCHDB_MIN_VERSION;

        List<HttpInteraction> interactions = new ArrayList<>();
        interactions.add(new GetCouchdbWelcomeInteraction(couchdbUriStr, welcomeMessage));
        interactions.add(new GetTokensDatabaseInteraction(couchdbUriStr + "/" + CouchdbAuthStore.TOKENS_DATABASE_NAME, HttpStatus.SC_OK));


        // We are expecting this to ne returned from our mock couchdb server to the code:
        //   "_id": "_design/docs",
        //   "_rev": "3-9e69612124f138c029ab40c9c9072deb",
        //   "views": {
        //     "loginId-view": {
        //       "map": "function (doc) {\n  if (doc.owner && doc.owner.loginId) {\n    emit(doc.owner.loginId, doc);\n  }\n}"
        //     }
        //   },
        //   "language": "javascript"
        // }
        TokenDBLoginView view = new TokenDBLoginView();
        view.map = "function (doc) {\n  if (doc.owner && doc.owner.loginId) {\n    emit(doc.owner.loginId, doc);\n  }\n}";
        TokenDBViews views = new TokenDBViews();
        views.loginIdView = view;
        TokensDBNameViewDesign designDocToPassBack = new TokensDBNameViewDesign();
        designDocToPassBack.language = "javascript";


        String tokensDesignDocUrl = couchdbUriStr + "/" + CouchdbAuthStore.TOKENS_DATABASE_NAME + "/_design/docs";
        interactions.add(new GetTokensDatabaseDesignInteraction(tokensDesignDocUrl, designDocToPassBack, HttpStatus.SC_INTERNAL_SERVER_ERROR));
        CloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        // When...
        CouchdbException thrown = catchThrowableOfType(
            () -> validator.checkCouchdbDatabaseIsValid(couchdbUri, mockHttpClient, new HttpRequestFactoryImpl()),
            CouchdbException.class
        );

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("Validation failed of database galasa_tokens design document");
    }

    @Test
    public void testCheckCouchdbDatabaseIsValidWithUpdateDesignDocResponseThrowsError() throws Exception {
        // Given...
        String couchdbUriStr = "https://my-couchdb-server";
        URI couchdbUri = URI.create(couchdbUriStr);
        CouchdbAuthStoreValidator validator = new CouchdbAuthStoreValidator();

        Welcome welcomeMessage = new Welcome();
        welcomeMessage.couchdb = "Welcome";
        welcomeMessage.version = CouchdbBaseValidator.COUCHDB_MIN_VERSION;

        List<HttpInteraction> interactions = new ArrayList<>();
        interactions.add(new GetCouchdbWelcomeInteraction(couchdbUriStr, welcomeMessage));
        interactions.add(new GetTokensDatabaseInteraction(couchdbUriStr + "/" + CouchdbAuthStore.TOKENS_DATABASE_NAME, HttpStatus.SC_OK));


        // We are expecting this to ne returned from our mock couchdb server to the code:
        //   "_id": "_design/docs",
        //   "_rev": "3-9e69612124f138c029ab40c9c9072deb",
        //   "views": {
        //     "loginId-view": {
        //       "map": "function (doc) {\n  if (doc.owner && doc.owner.loginId) {\n    emit(doc.owner.loginId, doc);\n  }\n}"
        //     }
        //   },
        //   "language": "javascript"
        // }
        TokenDBLoginView view = new TokenDBLoginView();
        view.map = "function (doc) {\n  if (doc.owner && doc.owner.loginId) {\n    emit(doc.owner.loginId, doc);\n  }\n}";
        TokenDBViews views = new TokenDBViews();
        views.loginIdView = view;
        TokensDBNameViewDesign designDocToPassBack = new TokensDBNameViewDesign();
        designDocToPassBack.language = "javascript";


        String tokensDesignDocUrl = couchdbUriStr + "/" + CouchdbAuthStore.TOKENS_DATABASE_NAME + "/_design/docs";
        interactions.add(new GetTokensDatabaseDesignInteraction(tokensDesignDocUrl, designDocToPassBack, HttpStatus.SC_OK));
        interactions.add(new UpdateTokensDatabaseDesignInteraction(tokensDesignDocUrl, "", HttpStatus.SC_INTERNAL_SERVER_ERROR));
        CloseableHttpClient mockHttpClient = new MockCloseableHttpClient(interactions);

        // When...
        CouchdbException thrown = catchThrowableOfType(
            () -> validator.checkCouchdbDatabaseIsValid(couchdbUri, mockHttpClient, new HttpRequestFactoryImpl()),
            CouchdbException.class
        );

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("Update of galasa_tokens design");
    }
    
}
