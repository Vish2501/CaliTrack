package com.calitrack.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "calitrack.coach.async-enabled", havingValue = "true")
public class KafkaTracingConfig {

    /**
     * With observation on, KafkaTemplate tags each send span with the Kafka cluster ID, which it
     * looks up through KafkaAdmin on the calling thread. When the broker is unreachable that lookup
     * blocked the finish-workout request for about a minute, on top of the 5s send timeout. Give the
     * template an admin that skips the lookup; the span just has no cluster ID tag, and trace context
     * still travels in the record headers.
     */
    @Bean
    static BeanPostProcessor kafkaTemplateWithoutClusterIdLookup() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof KafkaTemplate<?, ?> template) {
                    template.setKafkaAdmin(new KafkaAdmin(Map.of()) {
                        @Override
                        public String clusterId() {
                            return null;
                        }
                    });
                }
                return bean;
            }
        };
    }
}
