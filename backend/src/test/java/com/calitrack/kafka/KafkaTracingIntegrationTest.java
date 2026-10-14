package com.calitrack.kafka;

import com.calitrack.repository.CoachRecommendationRepository;
import com.calitrack.support.AbstractKafkaIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;

import static com.calitrack.config.KafkaTopicConfig.WORKOUT_LOGGED_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(KafkaTracingIntegrationTest.InMemorySpans.class)
class KafkaTracingIntegrationTest extends AbstractKafkaIntegrationTest {

    // Spring Boot hands every SpanExporter bean to the OpenTelemetry SDK, so the test can read
    // exactly the spans that would be sent to Jaeger.
    @TestConfiguration
    static class InMemorySpans {
        @Bean
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }
    }

    @Autowired
    private InMemorySpanExporter spans;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CoachRecommendationRepository recommendationRepository;

    @Test
    void theConsumerContinuesTheTraceStartedByFinishingAWorkout() throws Exception {
        when(coachModelClient.isConfigured()).thenReturn(true);
        when(coachModelClient.complete(anyString())).thenReturn("""
            RECOMMENDATION: Keep going.
            FOCUS_AREA: Back
            NEXT_EXERCISE: Pull-Up
            """);

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

        await().atMost(Duration.ofSeconds(30)).until(() -> recommendationRepository.existsByWorkoutId(workoutId));

        // Spans are exported in batches, so wait for both sides of the Kafka hop to arrive.
        await().atMost(Duration.ofSeconds(30)).until(() ->
            kafkaSpan(SpanKind.PRODUCER).isPresent() && kafkaSpan(SpanKind.CONSUMER).isPresent());

        SpanData producer = kafkaSpan(SpanKind.PRODUCER).orElseThrow();
        SpanData consumer = kafkaSpan(SpanKind.CONSUMER).orElseThrow();
        SpanData finishRequest = spans.getFinishedSpanItems().stream()
            .filter(span -> span.getKind() == SpanKind.SERVER)
            .filter(span -> span.getTraceId().equals(producer.getTraceId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No HTTP span in the producer's trace: " + spans.getFinishedSpanItems()));

        assertThat(consumer.getTraceId()).isEqualTo(producer.getTraceId());
        assertThat(finishRequest.getName()).containsIgnoringCase("patch");
    }

    private Optional<SpanData> kafkaSpan(SpanKind kind) {
        return spans.getFinishedSpanItems().stream()
            .filter(span -> span.getKind() == kind)
            .filter(span -> span.getName().contains(WORKOUT_LOGGED_TOPIC))
            .findFirst();
    }
}
