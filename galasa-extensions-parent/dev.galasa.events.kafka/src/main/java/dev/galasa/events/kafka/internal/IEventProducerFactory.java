/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.events.kafka.internal;

import java.util.Properties;

import dev.galasa.framework.spi.EventsException;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;

public interface IEventProducerFactory {

    KafkaEventProducer createProducer(Properties properties, String topic) throws EventsException;

    Properties createProducerConfig(IConfigurationPropertyStoreService cps, String topic) throws KafkaException;
    
}
