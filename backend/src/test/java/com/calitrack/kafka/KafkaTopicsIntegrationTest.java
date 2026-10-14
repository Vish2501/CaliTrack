package com.calitrack.kafka;

import com.calitrack.support.AbstractIntegrationTest;
import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static com.calitrack.config.KafkaTopicConfig.WORKOUT_LOGGED_DLT;
import static com.calitrack.config.KafkaTopicConfig.WORKOUT_LOGGED_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "calitrack.coach.async-enabled=true")
class KafkaTopicsIntegrationTest extends AbstractIntegrationTest {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Test
    void workoutLoggedTopicAndDeadLetterTopicAreCreatedWithMatchingPartitions() {
        Map<String, TopicDescription> topics =
            kafkaAdmin.describeTopics(WORKOUT_LOGGED_TOPIC, WORKOUT_LOGGED_DLT);

        assertThat(topics.get(WORKOUT_LOGGED_TOPIC).partitions()).hasSize(3);
        assertThat(topics.get(WORKOUT_LOGGED_DLT).partitions())
            .hasSameSizeAs(topics.get(WORKOUT_LOGGED_TOPIC).partitions());
    }
}
