/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import static org.assertj.core.api.Assertions.*;

import java.util.*;

import org.junit.Test;

import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures;
import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures.CreateArtifactDocInteractionOK;
import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures.CreateTestDocInteractionOK;
import dev.galasa.ras.couchdb.internal.mocks.HttpInteraction;
import dev.galasa.ras.couchdb.internal.mocks.MockLogFactory;

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



    @Test
    public void TestCouchdbRasStore_ONE_ARTIFACT_PER_DOCUMENT_featureTrueStopsDefaultArtifactDocumentBeingCreated() throws Exception {

        // See if we can create a store...
        Map<String,String> props = new HashMap<String,String>();

        props.put( FeatureFlag.ONE_ARTIFACT_PER_DOCUMENT.getPropertyName() , Boolean.toString(true) );

        List<HttpInteraction> interactions = new ArrayList<HttpInteraction>();
        interactions.add( new CreateTestDocInteractionOK(CouchdbTestFixtures.rasUriStr , CouchdbTestFixtures.documentId1, "124") );
        // Normal operation would need this interaction also...
        // but we don't expect this to occur when the feature flag is turned on.
        // interactions.add( new CreateArtifactDocInteractionOK(CouchdbTestFixtures.rasUriStr , CouchdbTestFixtures.documentId1, CouchdbTestFixtures.documentRev1) );
        // When the feature is turned on, we don't expect an artifact document to appear.

        fixtures.createCouchdbRasStore(props,interactions, new MockLogFactory());
    }

    
}