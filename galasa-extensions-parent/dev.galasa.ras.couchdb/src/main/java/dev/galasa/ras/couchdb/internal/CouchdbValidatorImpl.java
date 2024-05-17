/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import dev.galasa.extensions.common.couchdb.pojos.Welcome;
import dev.galasa.extensions.common.api.HttpRequestFactory;
import dev.galasa.framework.spi.utils.GalasaGson;

public class CouchdbValidatorImpl implements CouchdbValidator {
    
    private final GalasaGson                         gson               = new GalasaGson();
    private final Log                          logger             = LogFactory.getLog(getClass());
    private       HttpRequestFactory requestFactory;

    public void checkCouchdbDatabaseIsValid( URI rasUri, CloseableHttpClient httpClient , HttpRequestFactory httpRequestFactory) throws CouchdbRasException {
       this.requestFactory = httpRequestFactory;
        HttpGet httpGet = requestFactory.getHttpGetRequest(rasUri.toString());

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {

            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CouchdbRasException("Validation failed to CouchDB server - " + statusLine.toString());
            }

            HttpEntity entity = response.getEntity();
            String welcomePayload = EntityUtils.toString(entity);
            Welcome welcome = gson.fromJson(welcomePayload, Welcome.class);
            if (!"Welcome".equals(welcome.couchdb) || welcome.version == null) {
                throw new CouchdbRasException("Validation failed to CouchDB server - invalid json response");
            }

            checkVersion(welcome.version, 3, 3, 3);
            checkDatabasePresent(httpClient, rasUri, 1, "galasa_run");
            checkDatabasePresent(httpClient, rasUri, 1, "galasa_log");
            checkDatabasePresent(httpClient, rasUri, 1, "galasa_artifacts");

            checkRunDesignDocument(httpClient, rasUri,1);

            checkIndex(httpClient, rasUri, 1, "galasa_run", "runName");
            checkIndex(httpClient, rasUri, 1, "galasa_run", "requestor");
            checkIndex(httpClient, rasUri, 1, "galasa_run", "queued");
            checkIndex(httpClient, rasUri, 1, "galasa_run", "testName");
            checkIndex(httpClient, rasUri, 1, "galasa_run", "bundle");
            checkIndex(httpClient, rasUri, 1, "galasa_run", "result");

            logger.debug("RAS CouchDB at " + rasUri.toString() + " validated");
        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new CouchdbRasException("Validation failed", e);
        }
    }



    private void checkDatabasePresent( CloseableHttpClient httpClient, URI rasUri, int attempts, String dbName) throws CouchdbRasException {
        HttpHead httpHead = requestFactory.getHttpHeadRequest(rasUri + "/" + dbName);

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

        HttpPut httpPut = requestFactory.getHttpPutRequest(rasUri + "/" + dbName);
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
                checkDatabasePresent(httpClient, rasUri, attempts, dbName);
                return;
            }

            if (statusLine.getStatusCode() != HttpStatus.SC_CREATED) {
                EntityUtils.consumeQuietly(response.getEntity());
                throw new CouchdbRasException(
                        "Create Database " + dbName + " failed on CouchDB server - " + statusLine.toString());
            }

            EntityUtils.consumeQuietly(response.getEntity());
        } catch (CouchdbRasException e) {
            throw e;
        } catch (Exception e) {
            throw new CouchdbRasException("Create database " + dbName + " failed", e);
        }
    }

    private void checkRunDesignDocument( CloseableHttpClient httpClient , URI rasUri , int attempts) throws CouchdbRasException {
        HttpGet httpGet = requestFactory.getHttpGetRequest(rasUri + "/galasa_run/_design/docs");

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

            HttpPut httpPut = requestFactory.getHttpPutRequest(rasUri + "/galasa_run/_design/docs");
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
                    checkRunDesignDocument(httpClient, rasUri, attempts);
                    return;
                }
                
                if (statusCode != HttpStatus.SC_CREATED) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    throw new CouchdbRasException(
                            "Update of galasa_run design document failed on CouchDB server - " + statusLine.toString());
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



    private void checkIndex(CloseableHttpClient httpClient, URI rasUri , int attempts, String dbName, String field) throws CouchdbRasException {
        HttpGet httpGet = requestFactory.getHttpGetRequest(rasUri + "/galasa_run/_index");

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

            HttpPost httpPost = requestFactory.getHttpPostRequest(rasUri + "/galasa_run/_index");
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
                    checkIndex(httpClient, rasUri, attempts, dbName, field);
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

}
