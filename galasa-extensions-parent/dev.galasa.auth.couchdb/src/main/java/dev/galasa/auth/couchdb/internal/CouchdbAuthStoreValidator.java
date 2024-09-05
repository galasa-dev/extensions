/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb.internal;

import java.net.URI;

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

import com.google.gson.JsonSyntaxException;

import dev.galasa.extensions.common.couchdb.CouchdbBaseValidator;
import dev.galasa.extensions.common.couchdb.CouchdbClashingUpdateException;
import dev.galasa.extensions.common.couchdb.CouchdbException;
import dev.galasa.auth.couchdb.internal.beans.*;
import dev.galasa.extensions.common.api.HttpRequestFactory;
import dev.galasa.framework.spi.utils.GalasaGson;
import dev.galasa.framework.spi.utils.ITimeService;

import static dev.galasa.auth.couchdb.internal.Errors.*;

public class CouchdbAuthStoreValidator extends CouchdbBaseValidator {

    private final Log logger = LogFactory.getLog(getClass());
    private final GalasaGson gson = new GalasaGson();

    // A couchDB view, it gets all the access tokens of a the user based on the loginId provided.
    public static final String DB_TABLE_TOKENS_DESIGN = "function (doc) { if (doc.owner && doc.owner.loginId) {emit(doc.owner.loginId, doc); } }";

    @Override
    public void checkCouchdbDatabaseIsValid(
        URI couchdbUri, 
        CloseableHttpClient httpClient,
        HttpRequestFactory httpRequestFactory,
        ITimeService timeService
    ) throws CouchdbException {

        // Perform the base CouchDB checks
        super.checkCouchdbDatabaseIsValid(couchdbUri, httpClient, httpRequestFactory, timeService);

        RetryableCouchdbUpdateOperationProcessor retryProcessor = new RetryableCouchdbUpdateOperationProcessor(timeService);
        
        retryProcessor.retryCouchDbUpdateOperation( 
            ()->{ tryToCheckAndUpdateCouchDBTokenView(couchdbUri, httpClient, httpRequestFactory); 
        });
        
        logger.debug("Auth Store CouchDB at " + couchdbUri.toString() + " validated");
    }

    private void tryToCheckAndUpdateCouchDBTokenView(URI couchdbUri, CloseableHttpClient httpClient,
            HttpRequestFactory httpRequestFactory) throws CouchdbException {
       
        validateDatabasePresent(couchdbUri, CouchdbAuthStore.TOKENS_DATABASE_NAME);
        checkTokensDesignDocument(httpClient, couchdbUri, 1);
    }

    public void checkTokensDesignDocument(CloseableHttpClient httpClient, URI couchdbUri, int attempts)
            throws CouchdbException {

        // Get the design document from couchdb
        String docJson = getTokenDesignDocument(httpClient, couchdbUri, attempts);

        TokensDBNameViewDesign tableDesign = parseTokenDesignFromJson(docJson);

        boolean isDesignUpdated = updateDesignDocToDesiredDesignDoc(tableDesign);

        if (isDesignUpdated) {
            updateTokenDesignDocument(httpClient, couchdbUri, attempts, tableDesign);
        }
    }

    private TokensDBNameViewDesign parseTokenDesignFromJson(String docJson) throws CouchdbException {
        TokensDBNameViewDesign tableDesign;
        try {
            tableDesign = gson.fromJson(docJson, TokensDBNameViewDesign.class);
        } catch (JsonSyntaxException ex) {
            throw new CouchdbException(ERROR_FAILED_TO_PARSE_COUCHDB_DESIGN_DOC.getMessage(ex.getMessage()), ex);
        }

        if (tableDesign == null) {
            tableDesign = new TokensDBNameViewDesign();
        }
        return tableDesign;
    }

    private boolean updateDesignDocToDesiredDesignDoc(TokensDBNameViewDesign tableDesign) {
        boolean isUpdated = false;

        if (tableDesign.views == null) {
            isUpdated = true;
            tableDesign.views = new TokenDBViews();
        }

        if (tableDesign.views.loginIdView == null) {
            isUpdated = true;
            tableDesign.views.loginIdView = new TokenDBLoginView();
        }

        if (tableDesign.views.loginIdView.map == null
                || !DB_TABLE_TOKENS_DESIGN.equals(tableDesign.views.loginIdView.map)) {
            isUpdated = true;
            tableDesign.views.loginIdView.map = DB_TABLE_TOKENS_DESIGN;
        }

        if (tableDesign.language == null || !tableDesign.language.equals("javascript")) {
            isUpdated = true;
            tableDesign.language = "javascript";
        }

        return isUpdated;
    }

    private String getTokenDesignDocument(CloseableHttpClient httpClient, URI couchdbUri, int attempts)
            throws CouchdbException {
        HttpRequestFactory requestFactory = super.getRequestFactory();
        HttpGet httpGet = requestFactory.getHttpGetRequest(couchdbUri + "/" + CouchdbAuthStore.TOKENS_DATABASE_NAME +"/_design/docs");

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

    private void updateTokenDesignDocument(CloseableHttpClient httpClient, URI couchdbUri, int attempts,
            TokensDBNameViewDesign tokenViewDesign) throws CouchdbException {
        HttpRequestFactory requestFactory = super.getRequestFactory();

        logger.info("Updating the galasa_tokens design document");

        HttpEntity entity = new StringEntity(gson.toJson(tokenViewDesign), ContentType.APPLICATION_JSON);

        HttpPut httpPut = requestFactory.getHttpPutRequest(couchdbUri + "/" + CouchdbAuthStore.TOKENS_DATABASE_NAME +"/_design/docs");
        httpPut.setEntity(entity);

        if (tokenViewDesign._rev != null) {
            httpPut.addHeader("ETaq", "\"" + tokenViewDesign._rev + "\"");
        }

        try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == HttpStatus.SC_CONFLICT) {
                // Someone possibly updated the document while we were thinking about it.
                // It was probably another instance of this exact code.
                throw new CouchdbClashingUpdateException(ERROR_FAILED_TO_UPDATE_COUCHDB_DESING_DOC_CONFLICT.toString());
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
}
