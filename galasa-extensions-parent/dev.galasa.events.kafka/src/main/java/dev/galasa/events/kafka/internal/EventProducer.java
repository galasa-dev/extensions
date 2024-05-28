/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package dev.galasa.events.kafka.internal;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import dev.galasa.framework.spi.Environment;

import org.apache.kafka.common.security.plain.PlainLoginModule;

public class EventProducer {

    private final KafkaProducer<String, String> producer;
    private final String topic;

    private final String token = "GALASA_EVENT_STREAMS_TOKEN";

    public EventProducer(String topic, Environment environment) {

        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        Properties properties = new Properties();
        properties.put("bootstrap.servers", "BOOTSTRAPSERVERS");
        properties.put("topic", topic);
        properties.put("key.serializer", StringSerializer.class.getName());
        properties.put("value.serializer", StringSerializer.class.getName());
        properties.put("sasl.jaas.config", PlainLoginModule.class.getName() + " required username=\"token\" password=\"" + environment.getenv(token) + "\";");
        properties.put("security.protocol", "SASL_SSL");
        properties.put("sasl.mechanism", "PLAIN");
        properties.put("ssl.protocol", "TLSv1.2");
        properties.put("ssl.enabled.protocols", "TLSv1.2");
        properties.put("ssl.endpoint.identification.algorithm", "HTTPS");
        properties.put("transactional.id", "transactional-id");

        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
        this.producer = producer;

        this.topic = topic;
    }

    public void sendEvent(IEvent event){
        producer.initTransactions();
        producer.beginTransaction();
        producer.send(new ProducerRecord<>(topic, event.getId() + " " + event.getTimestamp() + " The test is now " + event.getMessage()));
        producer.commitTransaction();
    }

    public void close(){
        producer.close();
    }
    
}
