/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.extensions.common.couchdb;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dev.galasa.extensions.common.couchdb.pojos.Welcome;
import dev.galasa.extensions.common.impl.HttpRequestFactory;
import dev.galasa.framework.spi.utils.GalasaGson;

public class CouchdbBaseValidator implements CouchdbValidator {

    private final GalasaGson gson = new GalasaGson();
    private final Log logger = LogFactory.getLog(getClass());

    protected HttpRequestFactory requestFactory;

    @Override
    public void checkCouchdbDatabaseIsValid(URI couchdbUri, CloseableHttpClient httpClient,
            HttpRequestFactory httpRequestFactory) throws CouchdbException {
        this.requestFactory = httpRequestFactory;
        HttpGet httpGet = requestFactory.getHttpGetRequest(couchdbUri.toString());

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {

            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CouchdbException("Validation failed to CouchDB server - " + statusLine.toString());
            }

            HttpEntity entity = response.getEntity();
            String welcomePayload = EntityUtils.toString(entity);
            Welcome welcome = gson.fromJson(welcomePayload, Welcome.class);
            if (!"Welcome".equals(welcome.couchdb) || welcome.version == null) {
                throw new CouchdbException("Validation failed to CouchDB server - invalid json response");
            }

            checkVersion(welcome.version, 3, 3, 3);
            logger.debug("CouchDB is at the correct version");

        } catch (Exception e) {
            throw new CouchdbException("Validation failed", e);
        }
    }

    protected void checkDatabasePresent(CloseableHttpClient httpClient, URI couchdbUri, int attempts, String dbName) throws CouchdbException {
        HttpHead httpHead = requestFactory.getHttpHeadRequest(couchdbUri + "/" + dbName);

        try (CloseableHttpResponse response = httpClient.execute(httpHead)) {
            StatusLine statusLine = response.getStatusLine();

            if (statusLine.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.info("CouchDB database '" + dbName + "' is missing, creating");
                createDatabase(httpClient, couchdbUri, dbName, attempts);
            } else if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new CouchdbException(
                        "Validation failed of database " + dbName + " - " + statusLine.toString());
            }
        } catch (CouchdbException e) {
            throw e;
        } catch (Exception e) {
            throw new CouchdbException("Validation failed", e);
        }
    }

    private void createDatabase(CloseableHttpClient httpClient, URI couchdbUri, String dbName, int attempts) throws CouchdbException {
        HttpPut httpPut = requestFactory.getHttpPutRequest(couchdbUri + "/" + dbName);
        try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == HttpStatus.SC_CONFLICT) {
                // Someone possibly updated
                attempts++;
                if (attempts > 10) {
                    throw new CouchdbException(
                            "Create Database " + dbName + " failed on CouchDB server due to conflicts, attempted 10 times");
                }
                Thread.sleep(1000 + new Random().nextInt(3000));
                checkDatabasePresent(httpClient, couchdbUri, attempts, dbName);
                return;
            }

            if (statusLine.getStatusCode() != HttpStatus.SC_CREATED) {
                EntityUtils.consumeQuietly(response.getEntity());
                throw new CouchdbException(
                        "Create Database " + dbName + " failed on CouchDB server - " + statusLine.toString());
            }

            EntityUtils.consumeQuietly(response.getEntity());
        } catch (CouchdbException e) {
            throw e;
        } catch (Exception e) {
            throw new CouchdbException("Create database " + dbName + " failed", e);
        }
    }

    private void checkVersion(String version, int minVersion, int minRelease, int minModification)
            throws CouchdbException {
        String minVRM = minVersion + "." + minRelease + "." + minModification;

        Pattern vrm = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");
        Matcher m = vrm.matcher(version);

        if (!m.find()) {
            throw new CouchdbException("Invalid CouchDB version " + version);
        }

        int actualVersion = 0;
        int actualRelease = 0;
        int actualModification = 0;

        try {
            actualVersion = Integer.parseInt(m.group(1));
            actualRelease = Integer.parseInt(m.group(2));
            actualModification = Integer.parseInt(m.group(3));
        } catch (NumberFormatException e) {
            throw new CouchdbException("Unable to determine CouchDB version " + version, e);
        }

        if (actualVersion > minVersion) {
            return;
        }

        if (actualVersion < minVersion) {
            throw new CouchdbException("CouchDB version " + version + " is below minimum " + minVRM);
        }

        if (actualRelease > minRelease) {
            return;
        }

        if (actualRelease < minRelease) {
            throw new CouchdbException("CouchDB version " + version + " is below minimum " + minVRM);
        }

        if (actualModification > minModification) {
            return;
        }

        if (actualModification < minModification) {
            throw new CouchdbException("CouchDB version " + version + " is below minimum " + minVRM);
        }
    }
}
