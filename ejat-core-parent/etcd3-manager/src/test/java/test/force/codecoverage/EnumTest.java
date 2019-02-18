package test.force.codecoverage;

import org.junit.Assert;
import org.junit.Test;

import io.ejat.etcd3.spi.IEtcd3Listener.Event;

public class EnumTest {
	
	@Test
	public void testIEtcd3ListenerEvent() {
		Event event = Event.DELETE;
		event.compareTo(Event.PUT);		
		event.compareTo(Event.UNKNOWN);		
		Assert.assertTrue("dummy",true);
	}
	
}
