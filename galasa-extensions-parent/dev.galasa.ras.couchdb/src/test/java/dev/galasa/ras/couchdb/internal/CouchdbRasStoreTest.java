/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import dev.galasa.ras.couchdb.internal.mocks.*;

import com.google.gson.Gson;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;

import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IRun;
import dev.galasa.framework.spi.utils.GalasaGsonBuilder;
import dev.galasa.ras.couchdb.internal.pojos.PutPostResponse;

public class CouchdbRasStoreTest {
    
    @Rule
    public TestName testName = new TestName();
    
    private CouchdbRasStore createCouchdbRasStore(Map<String,String> inputProps) throws Exception {

        Map<String,String> props = new HashMap<String,String>();
        if (inputProps != null) {
            props.putAll(inputProps);
        }

        MockConfigurationPropertyStoreService mockCps = new MockConfigurationPropertyStoreService(props);

        IRun mockIRun = new MockIRun("L10");

        IFramework mockFramework = new MockFramework() {
            @Override
            public IRun getTestRun() {
                return mockIRun;
            }     
            @Override
            public @NotNull IConfigurationPropertyStoreService getConfigurationPropertyService(
                    @NotNull String namespace) throws ConfigurationPropertyStoreException {
                assertThat(namespace).isEqualTo("couchdb");
                return mockCps;
            } 
        };

        URI rasURI = URI.create("http://my.uri");

        MockStatusLine statusLine = new MockStatusLine();
        statusLine.setStatusCode(HttpStatus.SC_CREATED);

        String documentId = "xyz127";
        String documentRev = "123";

        PutPostResponse responseTransportBean = new PutPostResponse();
        responseTransportBean.id = documentId;
        responseTransportBean.ok = true ;
        responseTransportBean.rev = documentRev;

        Gson gson = GalasaGsonBuilder.build();
        String updateMessagePayload = gson.toJson(responseTransportBean);

        HttpEntity entity = new MockHttpEntity(updateMessagePayload); 

        MockCloseableHttpResponse response = new MockCloseableHttpResponse();
        response.setStatusLine(statusLine);
        response.setEntity(entity);

        MockCloseableHttpClient mockHttpClient = new MockCloseableHttpClient(response);

        MockCouchdbValidator mockValidator = new MockCouchdbValidator();

        MockHttpClientFactory mockHttpClientFactory = new MockHttpClientFactory(mockHttpClient);
        
        CouchdbRasStore couchdbRasStore = new CouchdbRasStore(mockFramework, rasURI, mockHttpClientFactory, mockValidator);

        return couchdbRasStore;
    }

        
    // Creating the Ras store causes the test structure in the couchdb 
    @Test
    public void TestCanCreateCouchdbRasStoreOK() throws Exception {

        // See if we can create a store...
        createCouchdbRasStore(null);
    }

    @Test
    public void TestFeatureIsEnabledAndCanFindItIsEnabled() throws Exception {
        // Given...
        String propertyName = FeatureFlag.ONE_ARTIFACT_PER_DOCUMENT.getPropertyName();

        // Fake cps store...
        Map<String,String> props = new HashMap<String,String>();
        props.put(propertyName, Boolean.toString(true));

        CouchdbRasStore couchdbRasStore = createCouchdbRasStore(props);
        
        // When...
        boolean isFeatureEnabled = couchdbRasStore.isFeatureFlagOneArtifactPerDocumentEnabled();

        // Then...
        assertThat(isFeatureEnabled).isTrue();
    }

    @Test
    public void TestFeatureIsFalseAndCanFindItIsDisabled() throws Exception {
        // Given...
        String propertyName = FeatureFlag.ONE_ARTIFACT_PER_DOCUMENT.getPropertyName();

        // Fake cps store...
        Map<String,String> props = new HashMap<String,String>();
        props.put(propertyName, Boolean.toString(false));

        CouchdbRasStore couchdbRasStore = createCouchdbRasStore(props);
        
        // When...
        boolean isFeatureEnabled = couchdbRasStore.isFeatureFlagOneArtifactPerDocumentEnabled();

        // Then...
        assertThat(isFeatureEnabled).isFalse();
    }

    @Test
    public void TestFeatureIsMissingAndSoItIsDisabled() throws Exception {
        // Given...
        
        // Fake cps store...
        Map<String,String> props = new HashMap<String,String>();
        // Don't add the feature flag into the fake cps... 

        CouchdbRasStore couchdbRasStore = createCouchdbRasStore(props);
        
        // When...
        boolean isFeatureEnabled = couchdbRasStore.isFeatureFlagOneArtifactPerDocumentEnabled();

        // Then...
        assertThat(isFeatureEnabled).isFalse();
    }
    

}