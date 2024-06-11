/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.events.kafka.mocks;

import java.util.Properties;

import dev.galasa.events.kafka.internal.IEventProducerFactory;
import dev.galasa.events.kafka.internal.KafkaEventProducer;
import dev.galasa.events.kafka.internal.KafkaException;
import dev.galasa.extensions.mocks.MockEnvironment;
import dev.galasa.framework.spi.EventsException;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;

public class MockEventProducerFactory implements IEventProducerFactory {

    private MockEnvironment env;

    public MockEventProducerFactory(MockEnvironment env) {
        this.env = env;
    }

    @Override
    public KafkaEventProducer createProducer(Properties properties, String topic) throws EventsException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createProducer'");
    }

    @Override
    public Properties createProducerConfig(IConfigurationPropertyStoreService cps, String topic) throws KafkaException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createProducerConfig'");
    }
    
}
