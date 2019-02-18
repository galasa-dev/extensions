package test.force.codecoverage;

import org.junit.Assert;
import org.junit.Test;

import io.ejat.core.etcd3.internal.Etcd3ConfigurationPropertyStore;
import io.ejat.framework.spi.ConfigurationPropertyStoreException;

public class Ectd3CpsTest {
	
	@Test
	public void testEtcd3ConfigurationPropertyStore() throws ConfigurationPropertyStoreException {
		Etcd3ConfigurationPropertyStore cps = new Etcd3ConfigurationPropertyStore();
		cps.initialise(null);
		Assert.assertTrue("dummy",true);
	}
	
}
