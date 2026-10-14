package com.calitrack.kafka;

import com.calitrack.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Async coach on, but nothing listening on the bootstrap address.
@TestPropertySource(properties = {
    "calitrack.coach.async-enabled=true",
    "spring.kafka.bootstrap-servers=localhost:1",
    "spring.kafka.admin.auto-create=false"
})
class WorkoutLoggedBrokerDownIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void finishingAWorkoutStillSucceedsWhenKafkaIsUnreachable() throws Exception {
        String startResponse = mockMvc.perform(post("/api/v1/workouts")
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long workoutId = objectMapper.readTree(startResponse).get("id").asLong();

        Instant start = Instant.now();
        mockMvc.perform(patch("/api/v1/workouts/{id}/finish", workoutId)
                .with(jwt().jwt(j -> j.subject(TEST_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.endTime").isNotEmpty());

        // Bounded by max.block.ms (5s), not Kafka's 60s default.
        assertThat(Duration.between(start, Instant.now())).isLessThan(Duration.ofSeconds(10));
    }
}
