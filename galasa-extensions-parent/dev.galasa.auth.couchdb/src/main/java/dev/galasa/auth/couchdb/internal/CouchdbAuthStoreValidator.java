/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb.internal;

import java.net.URI;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import dev.galasa.extensions.common.couchdb.CouchdbBaseValidator;
import dev.galasa.extensions.common.couchdb.CouchdbException;
import dev.galasa.extensions.common.api.HttpRequestFactory;
import dev.galasa.framework.spi.utils.GalasaGson;

public class CouchdbAuthStoreValidator extends CouchdbBaseValidator{

    private final Log logger = LogFactory.getLog(getClass());
    private final GalasaGson                         gson               = new GalasaGson();
    
    

    @Override
    public void checkCouchdbDatabaseIsValid(URI couchdbUri, CloseableHttpClient httpClient, HttpRequestFactory httpRequestFactory) throws CouchdbException {

        // Perform the base CouchDB checks
        super.checkCouchdbDatabaseIsValid(couchdbUri, httpClient, httpRequestFactory);
        validateDatabasePresent(couchdbUri, CouchdbAuthStore.TOKENS_DATABASE_NAME);
        checkTokensDesignDocument(httpClient, couchdbUri, 1);

        logger.debug("Auth Store CouchDB at " + couchdbUri.toString() + " validated");

    }
    
    public void checkTokensDesignDocument( CloseableHttpClient httpClient , URI couchdbUri , int attempts) throws CouchdbException {
        
        //Get the design document from couchdb
        String docJson = getTokenDesignDocument(httpClient, couchdbUri, attempts);
        
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

        JsonObject requestors = views.getAsJsonObject("loginId-view");
        if (requestors == null) {
            updated = true;
            requestors = new JsonObject();
            views.add("loginId-view", requestors);
        }

        if (isViewUpdated(requestors, "function (doc) { if (doc.owner && doc.owner.loginId) {emit(doc.owner.loginId, doc); } }", "javascript")) {
            updated = true;
        }

        if (updated) {
            updateTokenDesignDocument(httpClient, couchdbUri, attempts, doc, rev);
        }
    }

    private String getTokenDesignDocument(CloseableHttpClient httpClient , URI couchdbUri , int attempts) throws CouchdbException{
        HttpRequestFactory requestFactory = super.getRequestFactory();
        HttpGet httpGet = requestFactory.getHttpGetRequest(couchdbUri + "/galasa_tokens/_design/docs");

        String docJson = null;
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {

            StatusLine statusLine = response.getStatusLine();

            docJson = EntityUtils.toString(response.getEntity());
            if (statusLine.getStatusCode() != HttpStatus.SC_OK
                    && statusLine.getStatusCode() != HttpStatus.SC_NOT_FOUND) {
                throw new CouchdbException(
                        "Validation failed of database galasa_tokens design document - " + statusLine.toString());
            }
            if (statusLine.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                docJson = "{}";
            }

            return docJson;

        } catch (CouchdbException e) {
            throw e;
        } catch (Exception e) {
            throw new CouchdbException("Validation failed", e);
        }
    }

    private void updateTokenDesignDocument(CloseableHttpClient httpClient , URI couchdbUri , int attempts, JsonObject doc, String rev) throws CouchdbException{
        HttpRequestFactory requestFactory = super.getRequestFactory();

        logger.info("Updating the galasa_tokens design document");

            HttpEntity entity = new StringEntity(gson.toJson(doc), ContentType.APPLICATION_JSON);

            HttpPut httpPut = requestFactory.getHttpPutRequest(couchdbUri + "/galasa_tokens/_design/docs");
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
                        throw new CouchdbException(
                                "Update of galasa_token design document failed on CouchDB server due to conflicts, attempted 10 times");
                    }
                    Thread.sleep(1000 + new Random().nextInt(3000));
                    checkTokensDesignDocument(httpClient, couchdbUri, attempts);
                    return;
                }
                
                EntityUtils.consumeQuietly(response.getEntity());
                if (statusCode != HttpStatus.SC_CREATED) {
                    
                    throw new CouchdbException(
                            "Update of galasa_tokens design document failed on CouchDB server - " + statusLine.toString());
                }
                
            } catch (CouchdbException e) {
                throw e;
            } catch (Exception e) {
                throw new CouchdbException("Update of galasa_tokens design document failed", e);
            }
    }

    /**
     * @returns a boolean flag indicating if a couchdb view was updated. 
     */
    private boolean isViewUpdated(JsonObject view, String targetMap, String targetReduce) {

        boolean updated = false;

        if (isViewStringPresent(view, "map", targetMap)) {
            updated = true;
        }
        if (isViewStringPresent(view, "reduce", targetReduce)) {
            updated = true;
        }
        if (isViewStringPresent(view, "language", "javascript")) {
            updated = true;
        }

        return updated;
    }

    private boolean isViewStringPresent(JsonObject view, String field, String value) {

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
}
