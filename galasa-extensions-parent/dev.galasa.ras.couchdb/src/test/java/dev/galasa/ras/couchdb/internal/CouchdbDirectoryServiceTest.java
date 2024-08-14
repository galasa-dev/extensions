/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import dev.galasa.extensions.common.impl.HttpRequestFactoryImpl;
import dev.galasa.extensions.mocks.BaseHttpInteraction;
import dev.galasa.extensions.mocks.HttpInteraction;
import dev.galasa.framework.spi.IRunResult;
import dev.galasa.framework.spi.ResultArchiveStoreException;
import dev.galasa.framework.spi.ras.IRasSearchCriteria;
import dev.galasa.framework.spi.ras.RasSearchCriteriaQueuedFrom;
import dev.galasa.framework.spi.ras.RasSearchCriteriaQueuedTo;
import dev.galasa.framework.spi.ras.RasSearchCriteriaResult;
import dev.galasa.framework.spi.ras.RasSearchCriteriaRunName;
import dev.galasa.framework.spi.teststructure.TestStructure;
import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures;
import dev.galasa.ras.couchdb.internal.mocks.MockLogFactory;
import dev.galasa.ras.couchdb.internal.pojos.FoundRuns;
import dev.galasa.ras.couchdb.internal.pojos.TestStructureCouchdb;

public class CouchdbDirectoryServiceTest {

    private CouchdbTestFixtures fixtures = new CouchdbTestFixtures();

    class PostCouchdbFindRunsInteraction extends BaseHttpInteraction {

        private String[] expectedRequestBodyParts;

        public PostCouchdbFindRunsInteraction(String expectedUri, FoundRuns foundRuns, String... expectedRequestBodyParts) {
            this(expectedUri, HttpStatus.SC_OK, foundRuns, expectedRequestBodyParts);
        }

        public PostCouchdbFindRunsInteraction(String expectedUri, int statusCode, FoundRuns foundRuns, String... expectedRequestBodyParts) {
            super(expectedUri, statusCode);
            setResponsePayload(foundRuns);
            this.expectedRequestBodyParts = expectedRequestBodyParts;
        }

        @Override
        public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {
            super.validateRequest(host,request);
            assertThat(request.getRequestLine().getMethod()).isEqualTo("POST");
            if (expectedRequestBodyParts.length > 0) {
                validatePostRequestBody((HttpPost) request);
            }
        }

        private void validatePostRequestBody(HttpPost postRequest) {
            try {
                String requestBody = EntityUtils.toString(postRequest.getEntity());
                assertThat(requestBody).contains(expectedRequestBodyParts);

            } catch (IOException ex) {
                fail("Failed to parse POST request body");
            }
        }
    }

    private TestStructureCouchdb createRunTestStructure(String runName) {
        TestStructureCouchdb mockTestStructure = new TestStructureCouchdb();
        mockTestStructure._id = runName;
        mockTestStructure.setRunName(runName);
        return mockTestStructure;
    }

    @Test
    public void testGetRunsByQueuedFromOnePageReturnsRunsOk() throws Exception {
        // Given...
        TestStructureCouchdb mockRun1 = createRunTestStructure("run1");
        TestStructureCouchdb mockRun2 = createRunTestStructure("run2");

        Instant queuedFromTime = Instant.EPOCH;
        RasSearchCriteriaQueuedFrom queuedFrom = new RasSearchCriteriaQueuedFrom(queuedFromTime);

        FoundRuns findRunsResponse = new FoundRuns();
        findRunsResponse.docs = List.of(mockRun1, mockRun2);
        findRunsResponse.bookmark = "bookmark!";

        FoundRuns emptyRunsResponse = new FoundRuns();
        emptyRunsResponse.docs = new ArrayList<>();

        String expectedUri = "http://my.uri/galasa_run/_find";
        List<HttpInteraction> interactions = List.of(
            new PostCouchdbFindRunsInteraction(expectedUri, findRunsResponse, "queued", "$gte", queuedFromTime.toString()),
            new PostCouchdbFindRunsInteraction(expectedUri, emptyRunsResponse, "queued", "$gte", queuedFromTime.toString(), findRunsResponse.bookmark)
        );

        MockLogFactory mockLogFactory = new MockLogFactory();
        CouchdbRasStore mockRasStore = fixtures.createCouchdbRasStore(interactions, mockLogFactory);
        CouchdbDirectoryService directoryService = new CouchdbDirectoryService(mockRasStore, mockLogFactory, new HttpRequestFactoryImpl());

        // When...
        List<IRunResult> runs = directoryService.getRuns(queuedFrom);

        // Then...
        assertThat(runs).hasSize(2);
        assertThat(runs.get(0).getTestStructure().getRunName()).isEqualTo(mockRun1.getRunName());
        assertThat(runs.get(1).getTestStructure().getRunName()).isEqualTo(mockRun2.getRunName());
    }

    @Test
    public void testGetRunsMultiplePagesReturnsRunsOk() throws Exception {
        // Given...
        TestStructureCouchdb mockRun1 = createRunTestStructure("run1");
        TestStructureCouchdb mockRun2 = createRunTestStructure("run2");
        TestStructureCouchdb mockRun3 = createRunTestStructure("run3");

        Instant queuedFromTime = Instant.EPOCH;
        RasSearchCriteriaQueuedFrom queuedFrom = new RasSearchCriteriaQueuedFrom(queuedFromTime);

        FoundRuns findRunsResponsePage1 = new FoundRuns();
        findRunsResponsePage1.docs = List.of(mockRun1, mockRun2);
        findRunsResponsePage1.bookmark = "bookmark1";

        FoundRuns findRunsResponsePage2 = new FoundRuns();
        findRunsResponsePage2.docs = List.of(mockRun3);
        findRunsResponsePage2.bookmark = "bookmark2";

        FoundRuns emptyRunsResponse = new FoundRuns();
        emptyRunsResponse.docs = new ArrayList<>();

        String expectedUri = "http://my.uri/galasa_run/_find";
        List<HttpInteraction> interactions = List.of(
            new PostCouchdbFindRunsInteraction(expectedUri, findRunsResponsePage1, "queued", "$gte", queuedFromTime.toString()),
            new PostCouchdbFindRunsInteraction(expectedUri, findRunsResponsePage2, "queued", "$gte", queuedFromTime.toString(), findRunsResponsePage1.bookmark),
            new PostCouchdbFindRunsInteraction(expectedUri, emptyRunsResponse, "queued", "$gte", queuedFromTime.toString(), findRunsResponsePage2.bookmark)
        );

        MockLogFactory mockLogFactory = new MockLogFactory();
        CouchdbRasStore mockRasStore = fixtures.createCouchdbRasStore(interactions, mockLogFactory);
        CouchdbDirectoryService directoryService = new CouchdbDirectoryService(mockRasStore, mockLogFactory, new HttpRequestFactoryImpl());

        // When...
        List<IRunResult> runs = directoryService.getRuns(queuedFrom);

        // Then...
        assertThat(runs).hasSize(3);
        assertThat(runs.get(0).getTestStructure().getRunName()).isEqualTo(mockRun1.getRunName());
        assertThat(runs.get(1).getTestStructure().getRunName()).isEqualTo(mockRun2.getRunName());
        assertThat(runs.get(2).getTestStructure().getRunName()).isEqualTo(mockRun3.getRunName());
    }

    @Test
    public void testGetRunsWithInvalidRunIgnoresRunOk() throws Exception {
        // Given...
        String runName1 = "run1";
        TestStructureCouchdb mockRun1 = createRunTestStructure(runName1);

        // No run name is set, so this is not a valid run
        TestStructureCouchdb invalidRun = createRunTestStructure(null);

        FoundRuns findRunsResponse = new FoundRuns();
        findRunsResponse.docs = List.of(mockRun1, invalidRun);
        findRunsResponse.warning = "this response contains an invalid run!";
        findRunsResponse.bookmark = "bookmark!";

        FoundRuns emptyRunsResponse = new FoundRuns();
        emptyRunsResponse.docs = new ArrayList<>();

        String expectedUri = "http://my.uri/galasa_run/_find";
        List<HttpInteraction> interactions = List.of(
            new PostCouchdbFindRunsInteraction(expectedUri, findRunsResponse, "runName", runName1),
            new PostCouchdbFindRunsInteraction(expectedUri, emptyRunsResponse, "runName", runName1, findRunsResponse.bookmark)
        );

        MockLogFactory mockLogFactory = new MockLogFactory();
        CouchdbRasStore mockRasStore = fixtures.createCouchdbRasStore(interactions, mockLogFactory);
        CouchdbDirectoryService directoryService = new CouchdbDirectoryService(mockRasStore, mockLogFactory, new HttpRequestFactoryImpl());
        RasSearchCriteriaRunName runNameCriteria = new RasSearchCriteriaRunName(runName1);

        // When...
        List<IRunResult> runs = directoryService.getRuns(runNameCriteria);

        // Then...
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).getTestStructure().getRunName()).isEqualTo(mockRun1.getRunName());
    }

    @Test
    public void testGetRunsWithErrorResponseCodeThrowsError() throws Exception {
        // Given...
        String expectedUri = "http://my.uri/galasa_run/_find";
        List<HttpInteraction> interactions = List.of(
            new PostCouchdbFindRunsInteraction(expectedUri, HttpStatus.SC_INTERNAL_SERVER_ERROR, null)
        );

        MockLogFactory mockLogFactory = new MockLogFactory();
        CouchdbRasStore mockRasStore = fixtures.createCouchdbRasStore(interactions, mockLogFactory);
        CouchdbDirectoryService directoryService = new CouchdbDirectoryService(mockRasStore, mockLogFactory, new HttpRequestFactoryImpl());
        RasSearchCriteriaQueuedFrom queuedFrom = new RasSearchCriteriaQueuedFrom(Instant.EPOCH);

        // When...
        CouchdbRasException thrown = catchThrowableOfType(() -> directoryService.getRuns(queuedFrom), CouchdbRasException.class);

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("Unable to find runs");
    }

    @Test
    public void testGetRunsWithInvalidResponseThrowsError() throws Exception {
        // Given...
        FoundRuns findRunsResponse = new FoundRuns();
        findRunsResponse.docs = null;
        findRunsResponse.bookmark = "bookmark!";

        FoundRuns emptyRunsResponse = new FoundRuns();
        emptyRunsResponse.docs = new ArrayList<>();

        String expectedUri = "http://my.uri/galasa_run/_find";
        List<HttpInteraction> interactions = List.of(
            new PostCouchdbFindRunsInteraction(expectedUri, findRunsResponse),
            new PostCouchdbFindRunsInteraction(expectedUri, emptyRunsResponse)
        );

        MockLogFactory mockLogFactory = new MockLogFactory();
        CouchdbRasStore mockRasStore = fixtures.createCouchdbRasStore(interactions, mockLogFactory);
        CouchdbDirectoryService directoryService = new CouchdbDirectoryService(mockRasStore, mockLogFactory, new HttpRequestFactoryImpl());
        RasSearchCriteriaQueuedFrom queuedFrom = new RasSearchCriteriaQueuedFrom(Instant.EPOCH);

        // When...
        CouchdbRasException thrown = catchThrowableOfType(() -> directoryService.getRuns(queuedFrom), CouchdbRasException.class);

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("Unable to find runs", "Invalid JSON response");
    }

    @Test
    public void testGetRunsMultipleCriteriaReturnsRunsOk() throws Exception {
        // Given...
        TestStructureCouchdb mockRun1 = createRunTestStructure("run1");
        TestStructureCouchdb mockRun2 = createRunTestStructure("run2");

        Instant queuedFromTime = Instant.MAX;
        Instant queuedToTime = Instant.MAX;
        RasSearchCriteriaQueuedFrom queuedFrom = new RasSearchCriteriaQueuedFrom(queuedFromTime);
        RasSearchCriteriaQueuedTo queuedTo = new RasSearchCriteriaQueuedTo(queuedToTime);
        RasSearchCriteriaResult result = new RasSearchCriteriaResult("Passed");

        FoundRuns findRunsResponse = new FoundRuns();
        findRunsResponse.docs = List.of(mockRun1, mockRun2);
        findRunsResponse.bookmark = "bookmark!";

        FoundRuns emptyRunsResponse = new FoundRuns();
        emptyRunsResponse.docs = new ArrayList<>();

        String expectedUri = "http://my.uri/galasa_run/_find";
        String[] expectedRequestBodyParts = new String[] {
            "queued", "$gte", queuedFromTime.toString(), "$lt", queuedToTime.toString(), "result", "Passed"
        };

        List<HttpInteraction> interactions = List.of(
            new PostCouchdbFindRunsInteraction(expectedUri, findRunsResponse, expectedRequestBodyParts),
            new PostCouchdbFindRunsInteraction(expectedUri, emptyRunsResponse, expectedRequestBodyParts)
        );

        MockLogFactory mockLogFactory = new MockLogFactory();
        CouchdbRasStore mockRasStore = fixtures.createCouchdbRasStore(interactions, mockLogFactory);
        CouchdbDirectoryService directoryService = new CouchdbDirectoryService(mockRasStore, mockLogFactory, new HttpRequestFactoryImpl());

        // When...
        List<IRunResult> runs = directoryService.getRuns(queuedFrom, queuedTo, result);

        // Then...
        assertThat(runs).hasSize(2);
        assertThat(runs.get(0).getTestStructure().getRunName()).isEqualTo(mockRun1.getRunName());
        assertThat(runs.get(1).getTestStructure().getRunName()).isEqualTo(mockRun2.getRunName());
    }


    @Test
    public void testGetRunsWithInvalidCriteriaThrowsError() throws Exception {
        // Given...
        IRasSearchCriteria unknownCriteria = new IRasSearchCriteria() {
            @Override
            public boolean criteriaMatched(@NotNull TestStructure testStructure) {
                return false;
            }
        };

        List<HttpInteraction> interactions = new ArrayList<>();

        MockLogFactory mockLogFactory = new MockLogFactory();
        CouchdbRasStore mockRasStore = fixtures.createCouchdbRasStore(interactions, mockLogFactory);
        CouchdbDirectoryService directoryService = new CouchdbDirectoryService(mockRasStore, mockLogFactory, new HttpRequestFactoryImpl());

        // When...
        ResultArchiveStoreException thrown = catchThrowableOfType(() -> directoryService.getRuns(unknownCriteria), ResultArchiveStoreException.class);

        // Then...
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("Unrecognised search criteria");
    }
}
