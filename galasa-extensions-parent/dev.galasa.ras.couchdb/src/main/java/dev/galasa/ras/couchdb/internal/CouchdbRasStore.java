/*
* Copyright contributors to the Galasa project
*/
package dev.galasa.ras.couchdb.internal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.constraints.NotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IResultArchiveStoreDirectoryService;
import dev.galasa.framework.spi.IResultArchiveStoreService;
import dev.galasa.framework.spi.IRun;
import dev.galasa.framework.spi.ResultArchiveStoreException;
import dev.galasa.framework.spi.ras.ResultArchiveStoreFileStore;
import dev.galasa.framework.spi.teststructure.TestStructure;
import dev.galasa.framework.spi.utils.GalasaGsonBuilder;
import dev.galasa.ras.couchdb.internal.pojos.Artifacts;
import dev.galasa.ras.couchdb.internal.pojos.LogLines;
import dev.galasa.ras.couchdb.internal.pojos.PutPostResponse;
import dev.galasa.ras.couchdb.internal.pojos.Welcome;

public class CouchdbRasStore implements IResultArchiveStoreService {

    private final Log                          logger             = LogFactory.getLog(getClass());

    private final IFramework                   framework;                                         // NOSONAR
    private final URI                          rasUri;

    private final CloseableHttpClient          httpClient;
    private boolean                            shutdown           = false;

    private final Gson                         gson               = GalasaGsonBuilder.build();

    private final CouchdbRasFileSystemProvider provider;

    private final IRun                         run;
    private String                             runDocumentId;
    private String                             runDocumentRevision;

    private long                               logOrder           = 0;

    private final ArrayList<String>            logCache           = new ArrayList<>(100);

    private ArrayList<String>                  logIds             = new ArrayList<>();
    private ArrayList<String>                  artifactDocumentId = new ArrayList<>();;
    private String                             artifactDocumentRev;

    private TestStructure                      lastTestStructure;

    public CouchdbRasStore(IFramework framework, URI rasUri) throws CouchdbRasException {
        this.framework = framework;
        this.rasUri = rasUri;

        // *** Validate the connection to the server and it's version
        this.httpClient = HttpClients.createDefault();

        HttpGet httpGet = CouchdbRequests.getRequest(rasUri.toString());

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CouchdbRasException("Validation failed to CouchDB server - " + statusLine.toString());
            }

            HttpEntity entity = response.getEntity();
            Welcome welcome = gson.fromJson(EntityUtils.toString(entity), Welcome.class);
            if (!"Welcome".equals(welcome.getCouchdb()) || welcome.getVersion() == null) {
                throw new CouchdbRasException("Validation failed to CouchDB server - invalid json response");
            }

            checkVersion(welcome.getVersion(), 3, 3, 1);
            checkDatabasePresent(1,"_users");
            checkDatabasePresent(1,"_replicator");
            checkDatabasePresent(1, "galasa_run");
            checkDatabasePresent(1, "galasa_log");
            checkDatabasePresent(1, "galasa_artifacts");

            checkRunDesignDocument(1);

            checkIndex(1, "galasa_run", "runName");
            checkIndex(1, "galasa_run", "requestor");
            checkIndex(1, "galasa_run", "queued");
            checkIndex(1, "galasa_run", "testName");
            checkIndex(1, "galasa_run", "bundle");
            checkIndex(1, "galasa_run", "result");

            logger.debug("RAS CouchDB at " + this.rasUri.toString() + " validated");
        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new CouchdbRasException("Validation failed", e);
        }

        this.run = this.framework.getTestRun();

        // *** If this is a run, ensure we can create the run document
        if (this.run != null) {
            lastTestStructure = new TestStructure();
            lastTestStructure.setRunName(this.run.getName());
            try {
                updateTestStructure(lastTestStructure);
            } catch (ResultArchiveStoreException e) {
                throw new CouchdbRasException("Validation failed - unable to create initial run document", e);
            }

            createArtifactDocument();
        }

        ResultArchiveStoreFileStore fileStore = new ResultArchiveStoreFileStore();
        this.provider = new CouchdbRasFileSystemProvider(fileStore, this);
    }

    private void checkIndex(int attempts, String dbName, String field) throws CouchdbRasException {
        HttpGet httpGet = CouchdbRequests.getRequest(rasUri + "/galasa_run/_index");

        String idxJson = null;
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            StatusLine statusLine = response.getStatusLine();
            idxJson = EntityUtils.toString(response.getEntity());
            if (statusLine.getStatusCode() != HttpStatus.SC_OK
                    && statusLine.getStatusCode() != HttpStatus.SC_NOT_FOUND) {
                throw new CouchdbRasException("Validation failed of database indexes - " + statusLine.toString());
            }
            if (statusLine.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                idxJson = "{}";
            }
        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new CouchdbRasException("Validation failed", e);
        }

        JsonObject idx = gson.fromJson(idxJson, JsonObject.class);
        boolean create = true;

        String idxName = field + "-index";

        JsonArray idxs = idx.getAsJsonArray("indexes");
        if (idxs != null) {
            for (int i = 0; i < idxs.size(); i++) {
                JsonElement elem = idxs.get(i);
                if (elem.isJsonObject()) {
                    JsonObject o = (JsonObject) elem;

                    JsonElement name = o.get("name");
                    if (name != null) {
                        if (name.isJsonPrimitive() && ((JsonPrimitive) name).isString()) {
                            if (idxName.equals(name.getAsString())) {
                                create = false;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (create) {
            logger.info("Updating the galasa_run index " + idxName);

            JsonObject doc = new JsonObject();
            doc.addProperty("name", idxName);
            doc.addProperty("type", "json");

            JsonObject index = new JsonObject();
            doc.add("index", index);
            JsonArray fields = new JsonArray();
            index.add("fields", fields);

            JsonObject field1 = new JsonObject();
            fields.add(field1);
            field1.addProperty(field, "asc");

            HttpEntity entity = new StringEntity(gson.toJson(doc), ContentType.APPLICATION_JSON);

            HttpPost httpPost = CouchdbRequests.postRequest(rasUri + "/galasa_run/_index");
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                StatusLine statusLine = response.getStatusLine();
                EntityUtils.consumeQuietly(response.getEntity());
                int statusCode = statusLine.getStatusCode();
                if (statusCode == HttpStatus.SC_CONFLICT) {
                    // Someone possibly updated
                    attempts++;
                    if (attempts > 10) {
                        throw new CouchdbRasException(
                                "Update of galasa_run index failed on CouchDB server due to conflicts, attempted 10 times");
                    }
                    Thread.sleep(1000 + new Random().nextInt(3000));
                    checkIndex(attempts, dbName, field);
                    return;
                }

                if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                    throw new CouchdbRasException(
                            "Update of galasa_run index failed on CouchDB server - " + statusLine.toString());
                }

            } catch (CouchdbRasException e) {
                throw e;
            } catch (Exception e) {
                throw new CouchdbRasException("Update of galasa_run index faile", e);
            }
        }

    }

    private void checkDatabasePresent(int attempts, String dbName) throws CouchdbRasException {
        HttpHead httpHead = CouchdbRequests.headRequest(rasUri + "/" + dbName);       

        try (CloseableHttpResponse response = httpClient.execute(httpHead)) {
            StatusLine statusLine = response.getStatusLine();

            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                return;
            }
            if (statusLine.getStatusCode() != HttpStatus.SC_NOT_FOUND) {
                throw new CouchdbRasException(
                        "Validation failed of database " + dbName + " - " + statusLine.toString());
            }
        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new CouchdbRasException("Validation failed", e);
        }

        logger.info("CouchDB database " + dbName + " is missing,  creating");

        HttpPut httpPut = CouchdbRequests.putRequest(rasUri + "/" + dbName);


        try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == HttpStatus.SC_CONFLICT) {
                // Someone possibly updated
                attempts++;
                if (attempts > 10) {
                    throw new CouchdbRasException(
                            "Create Database " + dbName + " failed on CouchDB server due to conflicts, attempted 10 times");
                }
                Thread.sleep(1000 + new Random().nextInt(3000));
                checkDatabasePresent(attempts, dbName);
                return;
            }

            if (statusLine.getStatusCode() != HttpStatus.SC_CREATED) {
                EntityUtils.consumeQuietly(response.getEntity());
                throw new CouchdbRasException(
                        rasUri + "Create Database " + dbName + " failed on CouchDB server - " + statusLine.toString());
            }

            EntityUtils.consumeQuietly(response.getEntity());
        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new CouchdbRasException("Create database " + dbName + " failed", e);
        }
    }

    private void checkRunDesignDocument(int attempts) throws CouchdbRasException {
        HttpGet httpGet = CouchdbRequests.getRequest(rasUri + "/galasa_run/_design/docs");

        String docJson = null;
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            StatusLine statusLine = response.getStatusLine();
            docJson = EntityUtils.toString(response.getEntity());
            if (statusLine.getStatusCode() != HttpStatus.SC_OK
                    && statusLine.getStatusCode() != HttpStatus.SC_NOT_FOUND) {
                throw new CouchdbRasException(
                        "Validation failed of database galasa_run designdocument - " + statusLine.toString());
            }
            if (statusLine.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                docJson = "{}";
            }
        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new CouchdbRasException("Validation failed", e);
        }

        boolean updated = false;

        JsonObject doc = gson.fromJson(docJson, JsonObject.class);
        doc.remove("_id");
        String rev = null;
        if (doc.has("_rev")) {
            rev = doc.get("_rev").getAsString();
        }

        JsonObject views = doc.getAsJsonObject("views");
        if (views == null) {
            updated = true;
            views = new JsonObject();
            doc.add("views", views);
        }

        JsonObject requestors = views.getAsJsonObject("requestors-view");
        if (requestors == null) {
            updated = true;
            requestors = new JsonObject();
            views.add("requestors-view", requestors);
        }

        if (checkView(requestors, "function (doc) { emit(doc.requestor, 1); }", "_count")) {
            updated = true;
        }
        
        JsonObject result = views.getAsJsonObject("result-view");
        if (result == null) {
            updated = true;
            result = new JsonObject();
            views.add("result-view", result);
        }

        if (checkView(result, "function (doc) { emit(doc.result, 1); }", "_count")) {
            updated = true;
        }

        JsonObject testnames = views.getAsJsonObject("testnames-view");
        if (testnames == null) {
            updated = true;
            testnames = new JsonObject();
            views.add("testnames-view", testnames);
        }

        if (checkView(testnames, "function (doc) { emit(doc.testName, 1); }", "_count")) {
            updated = true;
        }

        JsonObject bundleTestnames = views.getAsJsonObject("bundle-testnames-view");
        if (bundleTestnames == null) {
            updated = true;
            bundleTestnames = new JsonObject();
            views.add("bundle-testnames-view", testnames);
        }

        if (checkView(bundleTestnames, "function (doc) { emit(doc.bundle + '/' + doc.testName, 1); }", "_count")) {
            updated = true;
        }

        if (updated) {
            logger.info("Updating the galasa_run design document");

            HttpEntity entity = new StringEntity(gson.toJson(doc), ContentType.APPLICATION_JSON);

            HttpPut httpPut = CouchdbRequests.putRequest(rasUri + "/galasa_run/_design/docs");
            httpPut.setEntity(entity);

            if (rev != null) {
                httpPut.addHeader("ETaq", "\"" + rev + "\"");
            }

            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == HttpStatus.SC_CONFLICT) {
                    // Someone possibly updated
                    attempts++;
                    if (attempts > 10) {
                        throw new CouchdbRasException(
                                "Update of galasa_run design document failed on CouchDB server due to conflicts, attempted 10 times");
                    }
                    Thread.sleep(1000 + new Random().nextInt(3000));
                    checkRunDesignDocument(attempts);
                    return;
                }
                
                if (statusCode != HttpStatus.SC_CREATED) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    throw new CouchdbRasException(
                            "Update of galasa_run design document failed on CouchDB server - " + statusLine.toString()+doc);
                }

                EntityUtils.consumeQuietly(response.getEntity());
            } catch (CouchdbRasException e) {
                throw e;
            } catch (Exception e) {
                throw new CouchdbRasException("Update of galasa_run design document faile", e);
            }
        }
    }

    private boolean checkView(JsonObject view, String targetMap, String targetReduce) {

        boolean updated = false;

        if (checkViewString(view, "map", targetMap)) {
            updated = true;
        }
        if (checkViewString(view, "reduce", targetReduce)) {
            updated = true;
        }
        if (checkViewString(view, "language", "javascript")) {
            updated = true;
        }

        return updated;
    }

    private boolean checkViewString(JsonObject view, String field, String value) {

        JsonElement element = view.get(field);
        if (element == null) {
            view.addProperty(field, value);
            return true;
        }

        if (!element.isJsonPrimitive() || !((JsonPrimitive) element).isString()) {
            view.addProperty(field, value);
            return true;
        }

        String actualValue = element.getAsString();
        if (!value.equals(actualValue)) {
            view.addProperty(field, value);
            return true;
        }
        return false;
    }

    private void checkVersion(String version, int minVersion, int minRelease, int minModification)
            throws CouchdbRasException {
        String minVRM = minVersion + "." + minRelease + "." + minModification;

        Pattern vrm = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");
        Matcher m = vrm.matcher(version);

        if (!m.find()) {
            throw new CouchdbRasException("Invalid CouchDB version " + version);
        }

        int actualVersion = 0;
        int actualRelease = 0;
        int actualModification = 0;

        try {
            actualVersion = Integer.parseInt(m.group(1));
            actualRelease = Integer.parseInt(m.group(2));
            actualModification = Integer.parseInt(m.group(3));
        } catch (NumberFormatException e) {
            throw new CouchdbRasException("Unable to determine CouchDB version " + version, e);
        }

        if (actualVersion > minVersion) {
            return;
        }

        if (actualVersion < minVersion) {
            throw new CouchdbRasException("CouchDB version " + version + " is below minimum " + minVRM);
        }

        if (actualRelease > minRelease) {
            return;
        }

        if (actualRelease < minRelease) {
            throw new CouchdbRasException("CouchDB version " + version + " is below minimum " + minVRM);
        }

        if (actualModification > minModification) {
            return;
        }

        if (actualModification < minModification) {
            throw new CouchdbRasException("CouchDB version " + version + " is below minimum " + minVRM);
        }

        return;

    }

    private void createArtifactDocument() throws CouchdbRasException {
        Artifacts artifacts = new Artifacts();
        artifacts.runId = this.runDocumentId;
        artifacts.runName = this.run.getName();

        String jsonArtifacts = gson.toJson(artifacts);

        HttpPost request = CouchdbRequests.postRequest(this.rasUri + "/galasa_artifacts");
        request.setEntity(new StringEntity(jsonArtifacts, StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_CREATED) {
                throw new CouchdbRasException("Unable to store the artifacts document - " + statusLine.toString());
            }

            HttpEntity entity = response.getEntity();
            PutPostResponse putPostResponse = gson.fromJson(EntityUtils.toString(entity), PutPostResponse.class);
            if (putPostResponse.id == null || putPostResponse.rev == null) {
                throw new CouchdbRasException("Unable to store the artifacts document - Invalid JSON response");
            }

            this.artifactDocumentId.add(putPostResponse.id);
            this.artifactDocumentRev = putPostResponse.rev;
        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new CouchdbRasException("Unable to store the artifacts document", e);
        }
    }

    @Override
    public void writeLog(@NotNull String message) throws ResultArchiveStoreException {
        if (this.run == null) {
            throw new ResultArchiveStoreException("Not a run");
        }

        String[] lines = message.split("\r\n?|\n");

        synchronized (logCache) {
            logCache.addAll(Arrays.asList(lines));
            if (logCache.size() >= 100) {
                flushLogCache();
            }
        }
    }

    private void flushLogCache() throws ResultArchiveStoreException {
        LogLines logLines = new LogLines();
        synchronized (logCache) {
            if (logCache.isEmpty()) {
                return;
            }
            logLines.lines = new ArrayList<>(logCache);
            logOrder++;
            logLines.order = logOrder;

            logCache.clear();
        }
        logLines.runName = this.run.getName();
        logLines.runId = this.runDocumentId;

        String jsonStructure = gson.toJson(logLines);

        HttpPost request = CouchdbRequests.postRequest(this.rasUri + "/galasa_log");
        request.setEntity(new StringEntity(jsonStructure, StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_CREATED) {
                throw new CouchdbRasException("Unable to store the test log - " + statusLine.toString());
            }

            HttpEntity entity = response.getEntity();
            PutPostResponse putPostResponse = gson.fromJson(EntityUtils.toString(entity), PutPostResponse.class);
            if (putPostResponse.id == null || putPostResponse.rev == null) {
                throw new CouchdbRasException("Unable to store the test log - Invalid JSON response");
            }

            this.logIds.add(putPostResponse.id);

            this.updateTestStructure(lastTestStructure);
        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new ResultArchiveStoreException("Unable to store the test log", e);
        }
    }

    @Override
    public void writeLog(@NotNull List<String> messages) throws ResultArchiveStoreException {
        if (this.run == null) {
            throw new ResultArchiveStoreException("Not a run");
        }

        for (String message : messages) {
            writeLog(message);
        }
    }

    @Override
    public synchronized void updateTestStructure(@NotNull TestStructure testStructure)
            throws ResultArchiveStoreException {
        if (this.run == null) {
            throw new ResultArchiveStoreException("Not a run");
        }

        this.lastTestStructure = testStructure;
        this.lastTestStructure.setLogRecordIds(this.logIds);
        this.lastTestStructure.setArtifactRecordIds(this.artifactDocumentId);
        this.lastTestStructure.normalise();

        String jsonStructure = gson.toJson(testStructure);

        HttpEntityEnclosingRequestBase request;
        if (runDocumentId == null) {
            request = CouchdbRequests.postRequest(this.rasUri + "/galasa_run");
        } else {
            request = CouchdbRequests.putRequest(this.rasUri + "/galasa_run/" + runDocumentId);
            request.addHeader("If-Match", runDocumentRevision);
        }
        request.setEntity(new StringEntity(jsonStructure, StandardCharsets.UTF_8));
        request.addHeader("Accept", "application/json");
        request.addHeader("Content-Type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_CREATED) {
                if (statusLine.getStatusCode() == HttpStatus.SC_CONFLICT) {
                    logger.error(
                            "The run document has been updated by another engine, terminating now to avoid corruption");
                    System.exit(0);
                }

                throw new CouchdbRasException("Unable to store the test structure - " + statusLine.toString());
            }

            HttpEntity entity = response.getEntity();
            PutPostResponse putPostResponse = gson.fromJson(EntityUtils.toString(entity), PutPostResponse.class);
            if (putPostResponse.id == null || putPostResponse.rev == null) {
                throw new CouchdbRasException("Unable to store the test structure - Invalid JSON response");
            }
            this.runDocumentId = putPostResponse.id;
            this.runDocumentRevision = putPostResponse.rev;

        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new ResultArchiveStoreException("Unable to store the test structure", e);
        }
    }

    public void retrieveArtifact(CouchdbArtifactPath path, Path cachePath) throws CouchdbRasException {
        String artifactRecordId = path.getArtifactRecordId();
        String encodedPath;
        try {
            encodedPath = URLEncoder.encode(path.toString(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new CouchdbRasException("Problem encoding artifact path", e);
        }

        HttpGet httpGet = CouchdbRequests.getRequest(this.rasUri + "/galasa_artifacts/" + artifactRecordId + "/" + encodedPath);

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                throw new CouchdbRasException("Not found - " + path.toString());
            }
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CouchdbRasException("Unable to find artifact - " + statusLine.toString());
            }

            HttpEntity entity = response.getEntity();
            Files.copy(entity.getContent(), cachePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new CouchdbRasException("Unable to find runs", e);
        }
    }

    public String getLog(TestStructure ts) throws CouchdbRasException {
        StringBuilder sb = new StringBuilder();

        for (String logRecordId : ts.getLogRecordIds()) {
            HttpGet httpGet = CouchdbRequests.getRequest(this.rasUri + "/galasa_log/" + logRecordId);
            httpGet.addHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_NOT_FOUND) { // TODO Ignore it for now
                    continue;
                }
                if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                    throw new CouchdbRasException("Unable to find logs - " + statusLine.toString());
                }

                HttpEntity entity = response.getEntity();
                String responseEntity = EntityUtils.toString(entity);
                LogLines logLines = gson.fromJson(responseEntity, LogLines.class);
                if (logLines.lines != null) {
                    for (String line : logLines.lines) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append(line);
                    }
                }
            } catch (CouchdbRasException e) {
                throw e;
            } catch (Exception e) {
                throw new CouchdbRasException("Unable to find runs", e);
            }
        }
        return sb.toString();
    }

    @Override
    public Path getStoredArtifactsRoot() {
        if (this.run == null) {
            return null;
        }
        return provider.getActualFileSystem().getPath("/");
    }

    @Override
    public void flush() {
        try {
            flushLogCache();
        } catch (ResultArchiveStoreException e) {
            logger.error("Error with heartbeat flush", e);
        }
    }

    @Override
    public void shutdown() {
        this.shutdown = true;
        try {
            flushLogCache();
        } catch (ResultArchiveStoreException e) {
            logger.error("Error with shutdown flush", e);
        }

        try {
            this.httpClient.close();
        } catch (IOException e) {
        }
    }

    protected boolean isShutdown() {
        return this.shutdown;
    }

    public CloseableHttpClient getHttpClient() {
        return this.httpClient;
    }

    public String getArtifactDocumentId() {
        return this.artifactDocumentId.get(0);
    }

    public String getArtifactDocumentRev() {
        return this.artifactDocumentRev;
    }

    public URI getCouchdbUri() {
        return this.rasUri;
    }

    public Gson getGson() {
        return this.gson;
    }

    public void updateArtifactDocumentRev(String newArtifactDocumentRev) {
        this.artifactDocumentRev = newArtifactDocumentRev;
    }

    @Override
    public @NotNull List<IResultArchiveStoreDirectoryService> getDirectoryServices() {
        ArrayList<IResultArchiveStoreDirectoryService> dirs = new ArrayList<>();
        dirs.add(new CouchdbDirectoryService(this));
        return dirs;
    }

    @Override
    public String calculateRasRunId() {
        
        if (this.runDocumentId == null) {
            return null;
        }
        return "cdb-" + this.runDocumentId;
    }

}
