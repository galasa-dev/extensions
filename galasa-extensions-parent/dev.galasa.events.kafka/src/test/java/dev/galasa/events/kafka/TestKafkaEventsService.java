/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.events.kafka;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import dev.galasa.events.kafka.internal.KafkaEventsService;
import dev.galasa.events.kafka.mocks.MockEventProducerFactory;
import dev.galasa.extensions.mocks.MockEnvironment;
import dev.galasa.extensions.mocks.cps.MockConfigurationPropertyStoreService;
import dev.galasa.framework.spi.ConfigurationPropertyStoreException;

public class TestKafkaEventsService {

    @Test
    public void TestCanCreateAKafkaEventsService() throws ConfigurationPropertyStoreException {
        // Given...
        Map<String,String> props = new HashMap<String,String>();
        props.put("bootstrap.servers", "broker1,broker2");

        MockConfigurationPropertyStoreService mockCps = new MockConfigurationPropertyStoreService(props);

        MockEnvironment mockEnv = new MockEnvironment();

        MockEventProducerFactory mockFactory = new MockEventProducerFactory(mockEnv);

        // Then...
        new KafkaEventsService(mockCps, mockFactory);
    }

    @Test
    public void TestCanProduceAnEventWithValidTopicAndValidEvent() throws Exception {
        // Given...
        Map<String,String> props = new HashMap<String,String>();
        props.put("bootstrap.servers", "broker1,broker2");

        MockConfigurationPropertyStoreService mockCps = new MockConfigurationPropertyStoreService(props);

        MockEnvironment mockEnv = new MockEnvironment();

        MockEventProducerFactory mockFactory = new MockEventProducerFactory(mockEnv);

        KafkaEventsService kafkaEventsService = new KafkaEventsService(mockCps, mockFactory);

        // When...
        kafkaEventsService.produceEvent(null, null);

        // Then...

    }

    @Test
    public void TestProduceEventWithInvalidTopicAndInvalidEventReturnsError() {

    }

    @Test
    public void TestCanShutdown() {

    }
    
}
