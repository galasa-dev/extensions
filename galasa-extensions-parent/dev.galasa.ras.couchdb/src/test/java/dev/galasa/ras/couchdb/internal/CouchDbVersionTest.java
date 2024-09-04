/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import java.util.*;
import static org.assertj.core.api.Assertions.*;

import org.apache.http.*;
import org.apache.http.client.methods.HttpPost;
import org.junit.*;
import org.junit.rules.TestName;

import dev.galasa.framework.spi.utils.GalasaGson;
import dev.galasa.extensions.common.couchdb.pojos.Welcome;
import dev.galasa.extensions.common.impl.HttpRequestFactoryImpl;
import dev.galasa.extensions.common.couchdb.CouchdbException;
import dev.galasa.extensions.common.couchdb.CouchdbValidator;
import dev.galasa.extensions.common.api.HttpRequestFactory;
import dev.galasa.extensions.mocks.*;
import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures;
import dev.galasa.ras.couchdb.internal.mocks.CouchdbTestFixtures.BaseHttpInteraction;;


public class CouchDbVersionTest {

    @Test
    public void testCanCreateAVersion() {
        CouchDbVersion version = new CouchDbVersion(1,2,3);
        assertThat(version.getVersion()).isEqualTo(1);
        assertThat(version.getRelease()).isEqualTo(2);
        assertThat(version.getModification()).isEqualTo(3);        
    }

    @Test
    public void testCanCreateAVersionFromAString() throws Exception {
        CouchDbVersion version = new CouchDbVersion("1.2.3");
        assertThat(version.getVersion()).isEqualTo(1);
        assertThat(version.getRelease()).isEqualTo(2);
        assertThat(version.getModification()).isEqualTo(3);        
    }

    @Test
    public void testInvalidVersionStringThrowsParsingError() throws Exception {
        CouchdbException ex = catchThrowableOfType( ()->{ new CouchDbVersion("1.2..3"); },
             CouchdbException.class );
        assertThat(ex).hasMessageContaining("1.2..3");
        // TODO: Assert that more of the message is in here.
    }

    @Test
    public void testInvalidLeadingDotVersionStringThrowsParsingError() throws Exception {
        CouchdbException ex = catchThrowableOfType( ()->{ new CouchDbVersion(".1.2.3"); },
             CouchdbException.class );
        assertThat(ex).hasMessageContaining(".1.2.3");
        // TODO: Assert that more of the message is in here.
    }


    @Test
    public void testCanICompareTheSameThingIsEqualToItself() throws Exception {
        CouchDbVersion version = new CouchDbVersion("1.2.3");
        assertThat(version.equals(version)).as("Could not compare the version with itself.").isTrue();
    }

    @Test
    public void testHashCodeOfTwoSameVersionsIsTheSame() throws Exception {
        CouchDbVersion version1 = new CouchDbVersion("1.2.3");
        CouchDbVersion version2 = new CouchDbVersion("1.2.3");
        assertThat(version1.hashCode()).isEqualTo(version2.hashCode());
    }

    @Test
    public void testTwoSameVersionsDifferentObjectsAreTheSame() throws Exception {
        CouchDbVersion version1 = new CouchDbVersion("1.2.3");
        CouchDbVersion version2 = new CouchDbVersion("1.2.3");
        assertThat(version1).isEqualTo(version2);
    }

    @Test
    public void testAddingTwoSameVersionsDifferentObjectsIntoASetResultInOneObjectInTheSet() throws Exception {
        CouchDbVersion version1 = new CouchDbVersion("1.2.3");
        CouchDbVersion version2 = new CouchDbVersion("1.2.3");
        Set<CouchDbVersion> mySet = new HashSet<>();
        mySet.add(version1);
        mySet.add(version2);

        assertThat(mySet).hasSize(1);
    }

    @Test
    public void testTwoDifferentVersionsAreNotComparable() throws Exception {
        CouchDbVersion version1 = new CouchDbVersion("1.2.3");
        CouchDbVersion version2 = new CouchDbVersion("1.2.4");

        int result = version1.compareTo(version2);

        assertThat(result).isNotEqualTo(0);
        assertThat(result).isEqualTo(-1);
    }


    public static class CouchDbVersionComparator implements Comparator<CouchDbVersion> {
        @Override
        public int compare(CouchDbVersion v1, CouchDbVersion v2) {
            return v1.compareTo(v2);
        }
    }

    @Test
    public void testToVersionsGetsSortedInCorrectOrderSmallestFirst() throws Exception {
        CouchDbVersion smallerVersion = new CouchDbVersion("1.2.3");
        CouchDbVersion biggerVersion  = new CouchDbVersion("1.2.4");
        List<CouchDbVersion> versions = new ArrayList<>();
        versions.addAll( List.of(smallerVersion, biggerVersion ));

        CouchDbVersionComparator comparator = new CouchDbVersionComparator();
        Collections.sort(versions, comparator);

        assertThat(versions.get(0)).isEqualTo(smallerVersion);
        assertThat(versions.get(1)).isEqualTo(biggerVersion);
    }

    @Test
    public void testToVersionsGetsSortedInCorrectOrderSmallestSecond() throws Exception {
        CouchDbVersion smallerVersion = new CouchDbVersion("1.2.3");
        CouchDbVersion biggerVersion  = new CouchDbVersion("1.2.4");
        List<CouchDbVersion> versions = new ArrayList<>();
        versions.addAll( List.of(biggerVersion, smallerVersion ));

        CouchDbVersionComparator comparator = new CouchDbVersionComparator();
        Collections.sort(versions, comparator);

        assertThat(versions.get(0)).isEqualTo(smallerVersion);
        assertThat(versions.get(1)).isEqualTo(biggerVersion);
    }

    @Test
    public void testCanCompareVersionsDirectly() throws Exception{
        CouchDbVersion smallerVersion = new CouchDbVersion("1.2.3");
        CouchDbVersion biggerVersion  = new CouchDbVersion("1.2.4");
        assertThat( smallerVersion).isLessThan(biggerVersion);
        assertThat( biggerVersion).isGreaterThan(smallerVersion);
    }

    @Test
    public void testCanCompareVersionsWhereVersionDiffers() throws Exception{
        CouchDbVersion smallerVersion = new CouchDbVersion("1.2.3");
        CouchDbVersion biggerVersion  = new CouchDbVersion("2.2.3");
        assertThat( smallerVersion).isLessThan(biggerVersion);
        assertThat( biggerVersion).isGreaterThan(smallerVersion);
    }

    @Test
    public void testCanCompareVersionsWhereReleaseDiffers() throws Exception{
        CouchDbVersion smallerVersion = new CouchDbVersion("1.2.3");
        CouchDbVersion biggerVersion  = new CouchDbVersion("1.3.3");
        assertThat( smallerVersion).isLessThan(biggerVersion);
        assertThat( biggerVersion).isGreaterThan(smallerVersion);
    }
}