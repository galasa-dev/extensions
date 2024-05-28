package dev.galasa.events.kafka.internal;

import dev.galasa.framework.spi.IEventsService;

public class KafkaEventsService implements IEventsService {

    @Override
    public void shutdown() {
        // Shutdown the event streams instance
    }
    
}
