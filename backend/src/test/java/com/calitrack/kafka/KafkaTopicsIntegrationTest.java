package com.calitrack.kafka;

import com.calitrack.support.AbstractKafkaIntegrationTest;
import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.Map;

import static com.calitrack.config.KafkaTopicConfig.WORKOUT_LOGGED_DLT;
import static com.calitrack.config.KafkaTopicConfig.WORKOUT_LOGGED_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicsIntegrationTest extends AbstractKafkaIntegrationTest {

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
