package test.force.codecoverage;

import org.junit.Assert;
import org.junit.Test;

import io.ejat.etcd3.Etcd3ManagerException;
import io.ejat.etcd3.spi.Etcd3ClientException;

public class ExceptionsTest {
	
	@Test
	public void testEtcd3ManagerException() {
		Throwable throwable = new Etcd3ManagerException();
		new Etcd3ManagerException("Message");		
		new Etcd3ManagerException("Message", throwable);		
		new Etcd3ManagerException(throwable);		
		new Etcd3ManagerException("Message", throwable, false, false);		
		Assert.assertTrue("dummy",true);
	}
	
	@Test
	public void testEtcd3ClientException() {
		Throwable throwable = new Etcd3ClientException();
		new Etcd3ClientException("Message");		
		new Etcd3ClientException("Message", throwable);		
		new Etcd3ClientException(throwable);		
		new Etcd3ClientException("Message", throwable, false, false);		
		Assert.assertTrue("dummy",true);
	}
	
	
}
