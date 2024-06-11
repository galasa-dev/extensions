/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.events.kafka.internal;

import dev.galasa.framework.spi.EventsException;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.framework.spi.IEventsService;
import dev.galasa.framework.spi.events.IEvent;

import java.util.Map;
import java.util.Properties;

public class KafkaEventsService implements IEventsService {

    private IConfigurationPropertyStoreService cps;

    private IEventProducerFactory producerFactory;

    // The EventProducers are cached so they can be reused for performance
    // Keyed on the name of the topic as one EventProducer is made for each topic
    // Note: Protected so unit tests can access this directly.
    protected Map<String, KafkaEventProducer> producers;

    public KafkaEventsService(IConfigurationPropertyStoreService cps, IEventProducerFactory producerFactory) {
        this.cps = cps;
        this.producerFactory = producerFactory;
    }

    @Override
    public void produceEvent(String topic, IEvent event) throws EventsException {

        if (topic == null || topic.isEmpty()) {
            throw new KafkaException("Topic is empty");
        }

        KafkaEventProducer producer;

        producer = producers.get(topic);
    
        if (producer == null) {

            synchronized (producers) {

                producer = producers.get(topic);
        
                if (producer == null) {
     
                    Properties properties = this.producerFactory.createProducerConfig(cps, topic);
                    
                    producer = this.producerFactory.createProducer(properties, topic);
                    producers.put(topic, producer);
                }
            }

        }

        producer.sendEvent(event);
    }

    @Override
    public void shutdown() {
        // Shut down all cached EventProducers
        for (Map.Entry<String, KafkaEventProducer> entry : producers.entrySet()) {
            entry.getValue().close();
        }
    }
    
}
