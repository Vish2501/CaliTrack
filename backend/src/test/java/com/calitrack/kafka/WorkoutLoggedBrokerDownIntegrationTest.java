package com.calitrack.kafka;

import com.calitrack.support.AbstractIntegrationTest;
import com.calitrack.support.FinishedWorkouts;
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
    "spring.kafka.admin.auto-create=false",
    "spring.kafka.listener.auto-startup=false"
})
class WorkoutLoggedBrokerDownIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void finishingAWorkoutStillSucceedsWhenKafkaIsUnreachable() throws Exception {
        Instant start = Instant.now();
        FinishedWorkouts.finish(mockMvc, TEST_USER);

        // Bounded by max.block.ms (5s), not Kafka's 60s default.
        assertThat(Duration.between(start, Instant.now())).isLessThan(Duration.ofSeconds(10));
    }
}
