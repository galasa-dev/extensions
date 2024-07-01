/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.events.kafka.internal;

import java.util.Properties;

import org.apache.kafka.common.security.plain.PlainLoginModule;
import org.apache.kafka.common.serialization.StringSerializer;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.EventsException;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;

public class KafkaEventProducerFactory implements IEventProducerFactory {

    private String authToken;

    public KafkaEventProducerFactory(String authToken) {
        this.authToken = authToken;
    }

    public KafkaEventProducer createProducer(Properties properties, String topic) throws EventsException {
        KafkaEventProducer eventProducer = new KafkaEventProducer(properties, topic);
        return eventProducer;
    }

    public Properties createProducerConfig(IConfigurationPropertyStoreService cps, String topic) throws KafkaException {
        Properties properties = new Properties();

        try {
            String bootstrapServers = cps.getProperty("bootstrap", "servers");

            // Needed to get the Kafka classes at runtime
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

            properties.put("bootstrap.servers", bootstrapServers);
            properties.put("topic", topic);
            properties.put("key.serializer", StringSerializer.class.getName());
            properties.put("value.serializer", StringSerializer.class.getName());
            properties.put("sasl.jaas.config", PlainLoginModule.class.getName() + " required username=\"token\" password=\"" + this.authToken + "\";");
            properties.put("security.protocol", "SASL_SSL");
            properties.put("sasl.mechanism", "PLAIN");
            properties.put("ssl.protocol", "TLSv1.2");
            properties.put("ssl.enabled.protocols", "TLSv1.2");
            properties.put("ssl.endpoint.identification.algorithm", "HTTPS");
            properties.put("transactional.id", "transactional-id");

        } catch (ConfigurationPropertyStoreException e) {
            throw new KafkaException("Unable to retrieve Kafka properties from the CPS", e);
        }

        return properties;
    }   
    
}
