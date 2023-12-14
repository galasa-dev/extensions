/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures;

public class CouchdbRasStoreTest {
    

    CouchdbTestFixtures fixtures = new CouchdbTestFixtures();    

        
    // Creating the Ras store causes the test structure in the couchdb 
    @Test
    public void TestCanCreateCouchdbRasStoreOK() throws Exception {

        // See if we can create a store...
        fixtures.createCouchdbRasStore(null);
    }

    @Test
    public void TestFeatureIsEnabledAndCanFindItIsEnabled() throws Exception {
        // Given...
        String propertyName = FeatureFlag.ONE_ARTIFACT_PER_DOCUMENT.getPropertyName();

        // Fake cps store...
        Map<String,String> props = new HashMap<String,String>();
        props.put(propertyName, Boolean.toString(true));

        CouchdbRasStore couchdbRasStore = fixtures.createCouchdbRasStore(props);
        
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

        CouchdbRasStore couchdbRasStore = fixtures.createCouchdbRasStore(props);
        
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

        CouchdbRasStore couchdbRasStore = fixtures.createCouchdbRasStore(props);
        
        // When...
        boolean isFeatureEnabled = couchdbRasStore.isFeatureFlagOneArtifactPerDocumentEnabled();

        // Then...
        assertThat(isFeatureEnabled).isFalse();
    }
    

}