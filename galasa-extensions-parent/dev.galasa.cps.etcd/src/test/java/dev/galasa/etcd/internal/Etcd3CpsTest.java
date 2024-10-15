/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.etcd.internal;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import dev.galasa.cps.etcd.internal.Etcd3ConfigurationPropertyRegistration;
import dev.galasa.cps.etcd.internal.Etcd3ConfigurationPropertyStore;
import dev.galasa.etcd.internal.mocks.MockEtcdClient;
import dev.galasa.framework.FrameworkInitialisation;
import dev.galasa.framework.spi.ConfigurationPropertyStoreException;

public class Etcd3CpsTest {

	@Test
    public void testGetCpsPropertyReturnsPropertyOk() throws Exception {
        // Given...
        Map<String, String> etcdContents = new HashMap<>();
        etcdContents.put("foo", "bar");

        MockEtcdClient mockEtcdClient = new MockEtcdClient(etcdContents);
        Etcd3ConfigurationPropertyStore store = new Etcd3ConfigurationPropertyStore(mockEtcdClient);

        // When...
        String out = store.getProperty("foo");

        // Then...
        assertThat(out).as("Unexpected Response").isEqualTo("bar");
    }

	@Test
    public void testGetNonExistantCpsPropertyReturnsNull() throws Exception {
        // Given...
        Map<String, String> etcdContents = new HashMap<>();
        etcdContents.put("foo", "bar");

        MockEtcdClient mockEtcdClient = new MockEtcdClient(etcdContents);
        Etcd3ConfigurationPropertyStore store = new Etcd3ConfigurationPropertyStore(mockEtcdClient);

        // When...
        String out = store.getProperty("non-existant");

        // Then...
        assertThat(out).as("Was not null as expected").isNull();
    }


	@Test
    public void testSetCpsPropertyAddsPropertyToStoreOk() throws Exception {
        // Given...
        Map<String, String> etcdContents = new HashMap<>();
        MockEtcdClient mockEtcdClient = new MockEtcdClient(etcdContents);
        Etcd3ConfigurationPropertyStore store = new Etcd3ConfigurationPropertyStore(mockEtcdClient);

        // When...
        store.setProperty("foo", "bar");

        // Then...
        assertThat(etcdContents).hasSize(1);
        assertThat(etcdContents.get("foo")).isEqualTo("bar");
    }

	@Test
    public void testDeleteCpsPropertyRemovesPropertyFromStoreOk() throws Exception {
        // Given...
        Map<String, String> etcdContents = new HashMap<>();
        etcdContents.put("foo", "bar");

        MockEtcdClient mockEtcdClient = new MockEtcdClient(etcdContents);
        Etcd3ConfigurationPropertyStore store = new Etcd3ConfigurationPropertyStore(mockEtcdClient);

        // When...
        assertThat(etcdContents).hasSize(1);
        store.deleteProperty("foo");

        // Then...
        assertThat(etcdContents).hasSize(0);
    }

    // @Ignore // Issue #1250 raised to re-add this test
    // @Test
    public void testRegistration() throws Exception {
        FrameworkInitialisation fi = Mockito.mock(FrameworkInitialisation.class);
        Etcd3ConfigurationPropertyRegistration regi = new Etcd3ConfigurationPropertyRegistration();

        when(fi.getBootstrapConfigurationPropertyStore()).thenReturn(new URI("etcd:http://thisIsALie"));
        regi.initialise(fi);

        assertTrue("dummy", true);
    }

    // @Ignore // Issue #1250 raised to re-add this test
    // @Test
    public void testRegistrationwithFile() throws ConfigurationPropertyStoreException, URISyntaxException {
        FrameworkInitialisation fi = Mockito.mock(FrameworkInitialisation.class);
        Etcd3ConfigurationPropertyRegistration regi = new Etcd3ConfigurationPropertyRegistration();

        when(fi.getBootstrapConfigurationPropertyStore()).thenReturn(new URI("file:///blah"));
        regi.initialise(fi);

        assertTrue("dummy", true);
    }
}
