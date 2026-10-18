package com.calitrack.kafka;

import com.calitrack.entity.CoachProfile;
import com.calitrack.entity.CoachRecommendation;
import com.calitrack.entity.Workout;
import com.calitrack.event.WorkoutLoggedEvent;
import com.calitrack.exception.NonRetryableCoachException;
import com.calitrack.repository.CoachProfileRepository;
import com.calitrack.repository.CoachRecommendationRepository;
import com.calitrack.repository.WorkoutRepository;
import com.calitrack.support.AbstractKafkaIntegrationTest;
import com.calitrack.support.FinishedWorkouts;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.calitrack.config.KafkaTopicConfig.WORKOUT_LOGGED_DLT;
import static com.calitrack.config.KafkaTopicConfig.WORKOUT_LOGGED_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Short backoff so retry tests finish quickly; the attempt count is the same as production.
@TestPropertySource(properties = "calitrack.coach.retry.initial-interval-ms=100")
class WorkoutLoggedConsumerIntegrationTest extends AbstractKafkaIntegrationTest {

    private static final String MODEL_REPLY = """
        RECOMMENDATION: Add a third set of pull-ups.
        FOCUS_AREA: Back
        NEXT_EXERCISE: Chin-Up
        """;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KafkaTemplate<String, WorkoutLoggedEvent> kafkaTemplate;

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private CoachRecommendationRepository recommendationRepository;

    @Autowired
    private CoachProfileRepository profileRepository;

    @BeforeEach
    void configureModel() {
        when(coachModelClient.isConfigured()).thenReturn(true);
    }

    @Test
    void finishingAWorkoutStoresARecommendationUsingTheUsersProfile() throws Exception {
        String userId = "e2e-user";
        CoachProfile profile = new CoachProfile();
        profile.setUserId(userId);
        profile.setGoals("strength");
        profile.setFitnessLevel("beginner");
        profileRepository.save(profile);
        when(coachModelClient.complete(anyString())).thenReturn(MODEL_REPLY);

        long workoutId = FinishedWorkouts.finish(mockMvc, userId);

        await().atMost(TIMEOUT).until(() -> recommendationRepository.existsByWorkoutId(workoutId));

        CoachRecommendation saved = recommendationRepository.findFirstByUserIdOrderByIdDesc(userId).orElseThrow();
        assertThat(saved.getWorkoutId()).isEqualTo(workoutId);
        assertThat(saved.getRecommendation()).isEqualTo("Add a third set of pull-ups.");
        assertThat(saved.getFocusArea()).isEqualTo("Back");
        assertThat(saved.getNextExercise()).isEqualTo("Chin-Up");

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(coachModelClient).complete(prompt.capture());
        assertThat(prompt.getValue()).contains("Goals: strength").contains("Fitness Level: beginner");
    }

    @Test
    void redeliveredEventDoesNotCallTheModelAgainOrDuplicateTheRecommendation() throws Exception {
        String userId = "redelivery-user";
        Workout workout = saveFinishedWorkout(userId);
        Workout marker = saveFinishedWorkout(userId);
        when(coachModelClient.complete(anyString())).thenReturn(MODEL_REPLY);

        send(eventFor(workout));
        send(eventFor(workout));
        // Same key, same partition: once the marker is processed, both deliveries above were too.
        send(eventFor(marker));

        await().atMost(TIMEOUT).until(() -> recommendationRepository.existsByWorkoutId(marker.getId()));

        assertThat(recommendationRepository.findAll())
            .filteredOn(r -> r.getWorkoutId().equals(workout.getId()))
            .hasSize(1);
        verify(coachModelClient, times(2)).complete(anyString());
    }

    @Test
    void transientFailureIsRetriedAndThenSucceeds() throws Exception {
        Workout workout = saveFinishedWorkout("retry-user");
        when(coachModelClient.complete(anyString()))
            .thenThrow(new ResourceAccessException("Read timed out"))
            .thenReturn(MODEL_REPLY);

        send(eventFor(workout));

        await().atMost(TIMEOUT).until(() -> recommendationRepository.existsByWorkoutId(workout.getId()));
        verify(coachModelClient, times(2)).complete(anyString());
    }

    @Test
    void quotaErrorSkipsRetriesAndGoesToTheDeadLetterTopic() throws Exception {
        Workout workout = saveFinishedWorkout("quota-user");
        when(coachModelClient.complete(anyString())).thenThrow(HttpClientErrorException.create(
            HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY,
            "{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\"}}".getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8));

        send(eventFor(workout));

        ConsumerRecord<String, String> dead = awaitDeadLetter("\"workoutId\":" + workout.getId());
        assertThat(header(dead, "kafka_dlt-exception-cause-fqcn"))
            .isEqualTo(NonRetryableCoachException.class.getName());
        verify(coachModelClient, times(1)).complete(anyString());
        assertThat(recommendationRepository.existsByWorkoutId(workout.getId())).isFalse();
    }

    @Test
    void persistentFailureGoesToTheDeadLetterTopicAfterThreeRetries() throws Exception {
        Workout workout = saveFinishedWorkout("outage-user");
        when(coachModelClient.complete(anyString())).thenThrow(new ResourceAccessException("Connection refused"));

        send(eventFor(workout));

        ConsumerRecord<String, String> dead = awaitDeadLetter("\"workoutId\":" + workout.getId());
        assertThat(dead.key()).isEqualTo("outage-user");
        verify(coachModelClient, times(4)).complete(anyString());
        assertThat(recommendationRepository.existsByWorkoutId(workout.getId())).isFalse();
    }

    @Test
    void malformedMessageGoesToTheDeadLetterTopicAndDoesNotBlockTheConsumer() throws Exception {
        String userId = "poison-user";
        Workout next = saveFinishedWorkout(userId);
        when(coachModelClient.complete(anyString())).thenReturn(MODEL_REPLY);

        try (KafkaProducer<String, String> raw = new KafkaProducer<>(
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()),
                new StringSerializer(), new StringSerializer())) {
            raw.send(new ProducerRecord<>(WORKOUT_LOGGED_TOPIC, userId, "not json")).get();
        }
        send(eventFor(next));

        ConsumerRecord<String, String> dead = awaitDeadLetter("not json");
        assertThat(dead.value()).isEqualTo("not json");
        await().atMost(TIMEOUT).until(() -> recommendationRepository.existsByWorkoutId(next.getId()));
    }

    private Workout saveFinishedWorkout(String userId) {
        Workout workout = new Workout();
        workout.setUserId(userId);
        workout.setClientId(UUID.randomUUID());
        workout.setStartTime(Instant.now().minus(Duration.ofHours(1)));
        workout.setEndTime(Instant.now());
        return workoutRepository.save(workout);
    }

    private WorkoutLoggedEvent eventFor(Workout workout) {
        return new WorkoutLoggedEvent(workout.getId(), workout.getUserId(), workout.getEndTime());
    }

    private void send(WorkoutLoggedEvent event) throws Exception {
        kafkaTemplate.send(WORKOUT_LOGGED_TOPIC, event.userId(), event).get();
    }

    private ConsumerRecord<String, String> awaitDeadLetter(String valueContains) {
        Map<String, Object> config = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
            ConsumerConfig.GROUP_ID_CONFIG, "dlt-reader-" + UUID.randomUUID(),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer =
                 new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(WORKOUT_LOGGED_DLT));
            List<ConsumerRecord<String, String>> found = new ArrayList<>();
            await().atMost(TIMEOUT).until(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(r -> {
                    if (r.value() != null && r.value().contains(valueContains)) {
                        found.add(r);
                    }
                });
                return !found.isEmpty();
            });
            return found.get(0);
        }
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
