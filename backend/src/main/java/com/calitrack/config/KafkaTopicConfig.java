package com.calitrack.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "calitrack.coach.async-enabled", havingValue = "true")
public class KafkaTopicConfig {

    public static final String WORKOUT_LOGGED_TOPIC = "workout-logged";
    // DeadLetterPublishingRecoverer's default destination is "<topic>.DLT".
    public static final String WORKOUT_LOGGED_DLT = WORKOUT_LOGGED_TOPIC + ".DLT";

    private static final int PARTITIONS = 3;

    @Bean
    public NewTopic workoutLoggedTopic() {
        return TopicBuilder.name(WORKOUT_LOGGED_TOPIC).partitions(PARTITIONS).replicas(1).build();
    }

    @Bean
    public NewTopic workoutLoggedDeadLetterTopic() {
        // The recoverer writes a failed record to the same partition number it came from,
        // so the DLT needs at least as many partitions as the source topic.
        return TopicBuilder.name(WORKOUT_LOGGED_DLT).partitions(PARTITIONS).replicas(1).build();
    }
}
