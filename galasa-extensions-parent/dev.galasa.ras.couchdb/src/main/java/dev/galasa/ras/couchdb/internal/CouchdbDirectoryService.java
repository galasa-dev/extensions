/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019,2020.
 */
package dev.galasa.ras.couchdb.internal;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;
import dev.galasa.framework.spi.IRunResult;
import dev.galasa.framework.spi.ResultArchiveStoreException;
import dev.galasa.framework.spi.ras.IRasSearchCriteria;
import dev.galasa.framework.spi.ras.RasSearchCriteriaQueuedFrom;
import dev.galasa.framework.spi.ras.RasSearchCriteriaQueuedTo;
import dev.galasa.framework.spi.ras.RasSearchCriteriaRequestor;
import dev.galasa.framework.spi.ras.RasSearchCriteriaTestName;
import dev.galasa.framework.spi.ras.RasTestClass;
import dev.galasa.framework.spi.ras.ResultArchiveStoreFileStore;
import dev.galasa.ras.couchdb.internal.pojos.Find;
import dev.galasa.ras.couchdb.internal.pojos.FoundRuns;
import dev.galasa.ras.couchdb.internal.pojos.Row;
import dev.galasa.ras.couchdb.internal.pojos.TestStructureCouchdb;
import dev.galasa.ras.couchdb.internal.pojos.ViewResponse;
import dev.galasa.ras.couchdb.internal.pojos.ViewRow;

public class CouchdbDirectoryService implements IResultArchiveStoreDirectoryService {

    private final Log             logger = LogFactory.getLog(getClass());

    private static final Charset  UTF8   = Charset.forName("utf-8");

    private final CouchdbRasStore store;

    public CouchdbDirectoryService(CouchdbRasStore store) {
        this.store = store;
    }

    @Override
    public @NotNull String getName() {
        return "CouchDB - " + store.getCouchdbUri();
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    private Path getRunArtifactPath(TestStructureCouchdb ts) throws CouchdbRasException {

        ResultArchiveStoreFileStore fileStore = new ResultArchiveStoreFileStore();
        CouchdbRasFileSystemProvider runProvider = new CouchdbRasFileSystemProvider(fileStore, store);
        if (ts.getArtifactRecordIds() == null || ts.getArtifactRecordIds().isEmpty()) {
            return runProvider.getRoot();
        }

        for (String artifactRecordId : ts.getArtifactRecordIds()) {
            HttpGet httpGet = new HttpGet(store.getCouchdbUri() + "/galasa_artifacts/" + artifactRecordId);
            httpGet.addHeader("Accept", "application/json");

            try (CloseableHttpResponse response = store.getHttpClient().execute(httpGet)) {
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_NOT_FOUND) { // TODO Ignore it for now
                    continue;
                }
                if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                    throw new CouchdbRasException("Unable to find artifacts - " + statusLine.toString());
                }

                HttpEntity entity = response.getEntity();
                String responseEntity = EntityUtils.toString(entity);
                JsonObject artifactRecord = store.getGson().fromJson(responseEntity, JsonObject.class);

                JsonElement attachmentsElement = artifactRecord.get("_attachments");

                if (attachmentsElement != null) {
                    if (attachmentsElement instanceof JsonObject) {
                        JsonObject attachments = (JsonObject) attachmentsElement;
                        Set<Entry<String, JsonElement>> entries = attachments.entrySet();
                        if (entries != null) {
                            for (Entry<String, JsonElement> entry : entries) {
                                JsonElement elem = entry.getValue();
                                if (elem instanceof JsonObject) {
                                    runProvider.addPath(new CouchdbArtifactPath(runProvider.getActualFileSystem(),
                                            entry.getKey(), (JsonObject) elem, artifactRecordId));
                                }
                            }
                        }
                    }
                }
            } catch (CouchdbRasException e) {
                throw e;
            } catch (Exception e) {
                throw new CouchdbRasException("Unable to find runs", e);
            }
        }

        return runProvider.getRoot();
    }

    private @NotNull List<IRunResult> getAllRuns() throws ResultArchiveStoreException {

        ArrayList<IRunResult> runs = new ArrayList<>();

        HttpGet httpGet = new HttpGet(store.getCouchdbUri() + "/galasa_run/_all_docs");
        httpGet.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = store.getHttpClient().execute(httpGet)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CouchdbRasException("Unable to find runs - " + statusLine.toString());
            }

            HttpEntity entity = response.getEntity();
            String responseEntity = EntityUtils.toString(entity);
            FoundRuns found = store.getGson().fromJson(responseEntity, FoundRuns.class);
            if (found.rows == null) {
                throw new CouchdbRasException("Unable to find rows - Invalid JSON response");
            }

            if (found.warning != null) {
                logger.warn("CouchDB warning detected - " + found.warning);
            }

            for (Row row : found.rows) {
                CouchdbRunResult cdbrr = fetchRun(row.id);
                if (cdbrr != null) {
                    runs.add(cdbrr);
                }
            }
        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new ResultArchiveStoreException("Unable to find runs", e);
        }

        return runs;
    }

    private CouchdbRunResult fetchRun(String id) throws ParseException, IOException, CouchdbRasException {
        HttpGet httpGet = new HttpGet(store.getCouchdbUri() + "/galasa_run/" + id);
        httpGet.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = store.getHttpClient().execute(httpGet)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                return null;
            }

            HttpEntity entity = response.getEntity();
            String responseEntity = EntityUtils.toString(entity);
            TestStructureCouchdb ts = store.getGson().fromJson(responseEntity, TestStructureCouchdb.class);

            Path runArtifactPath = getRunArtifactPath(ts);

            // *** Add this run to the results
            CouchdbRunResult cdbrr = new CouchdbRunResult(store, ts, runArtifactPath);
            return cdbrr;
        }
    }

    @Override
    public @NotNull List<String> getRequestors() throws ResultArchiveStoreException {
        ArrayList<String> requestors = new ArrayList<>();

        HttpGet httpGet = new HttpGet(
                store.getCouchdbUri() + "/galasa_run/_design/docs/_view/requestors-view?group=true");
        httpGet.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = store.getHttpClient().execute(httpGet)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CouchdbRasException("Unable to find runs - " + statusLine.toString());
            }

            HttpEntity entity = response.getEntity();
            String responseEntity = EntityUtils.toString(entity);
            ViewResponse view = store.getGson().fromJson(responseEntity, ViewResponse.class);
            if (view.rows == null) {
                throw new CouchdbRasException("Unable to find requestors - Invalid JSON response");
            }

            for (ViewRow row : view.rows) {
                requestors.add(row.key);
            }
        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new ResultArchiveStoreException("Unable to find requestors", e);
        }

        return requestors;
    }

    @Override
    public @NotNull List<String> getResultNames() throws ResultArchiveStoreException {
        ArrayList<String> results = new ArrayList<>();

        HttpGet httpGet = new HttpGet(
                store.getCouchdbUri() + "/galasa_run/_design/docs/_view/result-view?group=true");
        httpGet.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = store.getHttpClient().execute(httpGet)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CouchdbRasException("Unable to find results - " + statusLine.toString());
            }

            HttpEntity entity = response.getEntity();
            String responseEntity = EntityUtils.toString(entity);
            ViewResponse view = store.getGson().fromJson(responseEntity, ViewResponse.class);
            if (view.rows == null) {
                throw new CouchdbRasException("Unable to find results - Invalid JSON response");
            }

            for (ViewRow row : view.rows) {
                if (row.key != null) {
                    results.add(row.key);
                }
            }
        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new ResultArchiveStoreException("Unable to find results", e);
        }

        return results;

    }



    @Override
    public @NotNull List<RasTestClass> getTests() throws ResultArchiveStoreException {
        ArrayList<RasTestClass> tests = new ArrayList<>();

        HttpGet httpGet = new HttpGet(
                store.getCouchdbUri() + "/galasa_run/_design/docs/_view/bundle-testnames-view?group=true");
        httpGet.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = store.getHttpClient().execute(httpGet)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CouchdbRasException("Unable to find tests - " + statusLine.toString());
            }

            HttpEntity entity = response.getEntity();
            String responseEntity = EntityUtils.toString(entity);
            ViewResponse view = store.getGson().fromJson(responseEntity, ViewResponse.class);
            if (view.rows == null) {
                throw new CouchdbRasException("Unable to find rows - Invalid JSON response");
            }

            for (ViewRow row : view.rows) {
                String bundleTestname = row.key;
                if (bundleTestname == null) {
                    continue;
                }
                if ("undefined/undefined".equals(bundleTestname)) {
                    continue;
                }

                int posSlash = bundleTestname.indexOf('/');
                if (posSlash < 0) {
                    continue;
                }

                String bundleName = bundleTestname.substring(0, posSlash);
                String testName = bundleTestname.substring(posSlash + 1);

                RasTestClass rasTestClass = new RasTestClass(testName, bundleName);
                tests.add(rasTestClass);
            }
        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new ResultArchiveStoreException("Unable to find tests", e);
        }

        return tests;    
    }

    @Override
    public @NotNull List<IRunResult> getRuns(@NotNull IRasSearchCriteria... searchCriterias)
            throws ResultArchiveStoreException {

        if (searchCriterias.length == 0) {
            return getAllRuns();
        }

        ArrayList<IRunResult> runs = new ArrayList<>();

        HttpPost httpPost = new HttpPost(store.getCouchdbUri() + "/galasa_run/_find");

        JsonObject selector = new JsonObject();
        JsonArray and = new JsonArray();
        selector.add("$and", and);

        for(IRasSearchCriteria searchCriteria : searchCriterias) {
            if (searchCriteria instanceof RasSearchCriteriaRequestor) {
                RasSearchCriteriaRequestor sRequestor = (RasSearchCriteriaRequestor) searchCriteria;

                String[] requestors = sRequestor.getRequestors();
                if (requestors.length > 0) {
                    JsonArray jRequestors = new JsonArray();
                    for(String requestor : requestors) {
                        jRequestors.add(requestor);
                    }
                    JsonObject criteria = new JsonObject();
                    JsonObject jRequestor = new JsonObject();
                    jRequestor.add("$in", jRequestors);
                    criteria.add("requestor", jRequestor);
                    and.add(criteria);
                }
            } else if (searchCriteria instanceof RasSearchCriteriaQueuedFrom) {
                RasSearchCriteriaQueuedFrom sFrom = (RasSearchCriteriaQueuedFrom) searchCriteria;

                JsonObject criteria = new JsonObject();
                JsonObject jFrom = new JsonObject();
                jFrom.addProperty("$gte", sFrom.getFrom().toString());
                criteria.add("queued", jFrom);
                and.add(criteria);
            } else if (searchCriteria instanceof RasSearchCriteriaQueuedTo) {
                RasSearchCriteriaQueuedTo sTo = (RasSearchCriteriaQueuedTo) searchCriteria;

                JsonObject criteria = new JsonObject();
                JsonObject jTo = new JsonObject();
                jTo.addProperty("$lt", sTo.getTo().toString());
                criteria.add("queued", jTo);
                and.add(criteria);
            } else if (searchCriteria instanceof RasSearchCriteriaTestName) {
                RasSearchCriteriaTestName sTestName = (RasSearchCriteriaTestName) searchCriteria;

                String[] testNames = sTestName.getTestNames();
                if (testNames.length > 0) {
                    JsonArray jTestNames = new JsonArray();
                    for(String testName : testNames) {
                        jTestNames.add(testName);
                    }
                    JsonObject criteria = new JsonObject();
                    JsonObject jTestName = new JsonObject();
                    jTestName.add("$in", jTestNames);
                    criteria.add("testName", jTestName);
                    and.add(criteria);
                }
            } else {
                throw new ResultArchiveStoreException("Unrecognised search criteria class " + searchCriteria.getClass().getName());
            }
        }



        //      if (testName != null) {
        //          JsonObject criteria = new JsonObject();
        //          JsonObject jtestName = new JsonObject();
        //          jtestName.addProperty("$eq", testName.toString());
        //          criteria.add("testName", jtestName);  // TODO check property name
        //          and.add(criteria);
        //      }
        //
        Find find = new Find();
        find.selector = selector;
        find.execution_stats = true;

        httpPost.addHeader("Accept", "application/json");
        httpPost.addHeader("Content-Type", "application/json");

        while (true) {
            String requestContent = store.getGson().toJson(find);
            httpPost.setEntity(new StringEntity(requestContent, UTF8));

            try (CloseableHttpResponse response = store.getHttpClient().execute(httpPost)) {
                StatusLine statusLine = response.getStatusLine();
                HttpEntity entity = response.getEntity();
                String responseEntity = EntityUtils.toString(entity);

                if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                    throw new CouchdbRasException("Unable to find runs - " + statusLine.toString());
                }

                FoundRuns found = store.getGson().fromJson(responseEntity, FoundRuns.class);
                if (found.docs == null) {
                    throw new CouchdbRasException("Unable to find runs - Invalid JSON response");
                }

                if (found.warning != null) {
                    logger.warn("CouchDB warning detected - " + found.warning);
                }

                if (found.docs.isEmpty()) {
                    break;
                }

                for (TestStructureCouchdb ts : found.docs) {
                    Path runArtifactPath = getRunArtifactPath(ts);

                    // *** Add this run to the results
                    CouchdbRunResult cdbrr = new CouchdbRunResult(store, ts, runArtifactPath);
                    runs.add(cdbrr);
                }

                // *** find the next batch of runs
                find.bookmark = found.bookmark;
            } catch (CouchdbRasException e) {
                throw e;
            } catch (Exception e) {
                throw new ResultArchiveStoreException("Unable to find runs", e);
            }
        }

        return runs;
    }

}
