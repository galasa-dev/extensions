package test.force.codecoverage;

import org.junit.Test;

import io.ejat.core.etcd3.spi.IEtcd3Listener.Event;

public class EnumTest {
	
	@Test
	public void testIEtcd3ListenerEvent() {
		Event event = Event.DELETE;
		event.compareTo(Event.PUT);		
		event.compareTo(Event.UNKNOWN);		
		return;
	}
	
}
