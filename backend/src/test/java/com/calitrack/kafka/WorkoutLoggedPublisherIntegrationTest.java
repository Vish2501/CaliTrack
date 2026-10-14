package com.calitrack.kafka;

import com.calitrack.support.AbstractKafkaIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static com.calitrack.config.KafkaTopicConfig.WORKOUT_LOGGED_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkoutLoggedPublisherIntegrationTest extends AbstractKafkaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void finishingAWorkoutPublishesWorkoutLoggedKeyedByUser() throws Exception {
        String startResponse = mockMvc.perform(post("/api/v1/workouts")
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long workoutId = objectMapper.readTree(startResponse).get("id").asLong();

        mockMvc.perform(patch("/api/v1/workouts/{id}/finish", workoutId)
                .with(jwt().jwt(j -> j.subject(TEST_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk());

        ConsumerRecord<String, String> record = pollForWorkout(workoutId)
            .orElseThrow(() -> new AssertionError("No WorkoutLogged event for workout " + workoutId));

        assertThat(record.key()).isEqualTo(TEST_USER);
        // No Java type header: consumers read plain JSON.
        assertThat(record.headers().lastHeader("__TypeId__")).isNull();

        JsonNode payload = objectMapper.readTree(record.value());
        assertThat(payload.get("workoutId").asLong()).isEqualTo(workoutId);
        assertThat(payload.get("userId").asText()).isEqualTo(TEST_USER);
        assertThat(LocalDateTime.parse(payload.get("finishedAt").asText())).isNotNull();
    }

    private Optional<ConsumerRecord<String, String>> pollForWorkout(long workoutId) {
        Map<String, Object> config = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
            ConsumerConfig.GROUP_ID_CONFIG, "publisher-test",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer =
                 new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(WORKOUT_LOGGED_TOPIC));
            Instant deadline = Instant.now().plusSeconds(15);
            while (Instant.now().isBefore(deadline)) {
                Optional<ConsumerRecord<String, String>> match =
                    StreamSupport.stream(consumer.poll(Duration.ofMillis(500)).spliterator(), false)
                        .filter(r -> r.value().contains("\"workoutId\":" + workoutId))
                        .findFirst();
                if (match.isPresent()) {
                    return match;
                }
            }
        }
        return Optional.empty();
    }
}
