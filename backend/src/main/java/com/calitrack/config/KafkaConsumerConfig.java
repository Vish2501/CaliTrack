package com.calitrack.config;

import com.calitrack.exception.NonRetryableCoachException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "calitrack.coach.async-enabled", havingValue = "true")
public class KafkaConsumerConfig {

    /**
     * Picked up by Spring Boot's listener container factory. Retries a failed record with
     * exponential backoff (1s, 2s, 4s by default), then publishes it to "<topic>.DLT".
     * NonRetryableCoachException and deserialization failures skip the retries.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            ProducerFactory<Object, Object> producerFactory,
            @Value("${calitrack.coach.retry.initial-interval-ms}") long initialIntervalMs,
            @Value("${calitrack.coach.retry.max-retries}") int maxRetries) {
        ExponentialBackOff backOff = new ExponentialBackOffWithMaxRetries(maxRetries);
        backOff.setInitialInterval(initialIntervalMs);
        backOff.setMultiplier(2.0);

        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(deadLetterTemplate(producerFactory));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(NonRetryableCoachException.class);
        return errorHandler;
    }

    // A record that failed deserialization reaches the recoverer as its original byte[], which
    // must be written back unchanged; everything else is an event object written as JSON.
    private KafkaTemplate<String, Object> deadLetterTemplate(ProducerFactory<Object, Object> producerFactory) {
        // The producer factory configures each delegate from the producer properties, which
        // already set spring.json.add.type.headers=false. JsonSerializer refuses to be configured
        // both that way and through its setters, so no setters here.
        Map<Class<?>, Serializer<?>> delegates = new LinkedHashMap<>();
        delegates.put(byte[].class, new ByteArraySerializer());
        delegates.put(Object.class, new JsonSerializer<>());

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<String, Object>(
            producerFactory.getConfigurationProperties(),
            new StringSerializer(),
            new DelegatingByTypeSerializer(delegates, true)));
    }
}
