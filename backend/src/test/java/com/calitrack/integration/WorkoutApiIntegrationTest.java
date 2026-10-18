package com.calitrack.integration;

import com.calitrack.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkoutApiIntegrationTest extends AbstractIntegrationTest {

    private static final String STARTED = "2026-06-01T09:00:00Z";
    private static final String ENDED = "2026-06-01T10:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void finishSavesTheWorkoutWithItsSetsInTheOrderSent() throws Exception {
        String user = "order-user";
        long squat = createExercise(user);
        long pullUp = createExercise(user);

        String body = finish(user, workoutJson(UUID.randomUUID(), STARTED, ENDED, """
                [{"exerciseId":%d,"reps":5,"weight":100},
                 {"exerciseId":%d,"reps":8,"weight":0},
                 {"exerciseId":%d,"reps":4,"weight":105}]
                """.formatted(squat, pullUp, squat)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.startTime").value(STARTED))
            .andExpect(jsonPath("$.endTime").value(ENDED))
            .andReturn().getResponse().getContentAsString();
        long workoutId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/v1/workouts/{id}", workoutId)
                .with(jwt().jwt(j -> j.subject(user))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sets[*].reps", contains(5, 8, 4)))
            .andExpect(jsonPath("$.sets[*].exerciseId", contains((int) squat, (int) pullUp, (int) squat)));
    }

    @Test
    void finishingTheSameWorkoutAgainReturnsTheSavedOneAndSavesNothing() throws Exception {
        String user = "retry-user";
        String workout = workoutJson(UUID.randomUUID(), STARTED, ENDED, oneSet(createExercise(user)));

        String first = finish(user, workout).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long workoutId = objectMapper.readTree(first).get("id").asLong();

        finish(user, workout)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(workoutId))
            .andExpect(jsonPath("$.sets", hasSize(1)));

        mockMvc.perform(get("/api/v1/workouts").with(jwt().jwt(j -> j.subject(user))))
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void theWorkoutIdIsUniqueForEachUserNotGlobally() throws Exception {
        UUID sharedId = UUID.randomUUID();

        finish("first-user", workoutJson(sharedId, STARTED, ENDED, oneSet(createExercise("first-user"))))
            .andExpect(status().isCreated());
        finish("second-user", workoutJson(sharedId, STARTED, ENDED, oneSet(createExercise("second-user"))))
            .andExpect(status().isCreated());
    }

    @Test
    void anEndBeforeTheStartIsABadRequestAndSavesNothing() throws Exception {
        String user = "backwards-user";

        finish(user, workoutJson(UUID.randomUUID(), ENDED, STARTED, oneSet(createExercise(user))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("endedAt must not be before startedAt"));

        assertNoWorkouts(user);
    }

    @Test
    void aWorkoutWithNoSetsIsABadRequest() throws Exception {
        String user = "empty-user";

        finish(user, workoutJson(UUID.randomUUID(), STARTED, ENDED, "[]"))
            .andExpect(status().isBadRequest());

        assertNoWorkouts(user);
    }

    @Test
    void aSetWithAMissingValueIsABadRequestAndSavesNothing() throws Exception {
        String user = "missing-reps-user";
        long exerciseId = createExercise(user);

        finish(user, workoutJson(UUID.randomUUID(), STARTED, ENDED, """
                [{"exerciseId":%d,"reps":8,"weight":0},
                 {"exerciseId":%d,"weight":20}]
                """.formatted(exerciseId, exerciseId)))
            .andExpect(status().isBadRequest());

        assertNoWorkouts(user);
    }

    @Test
    void aSetForAnotherUsersExerciseIsABadRequestAndSavesNothing() throws Exception {
        String user = "borrowing-user";
        long own = createExercise(user);
        long someoneElses = createExercise(OTHER_USER);

        finish(user, workoutJson(UUID.randomUUID(), STARTED, ENDED, """
                [{"exerciseId":%d,"reps":8,"weight":0},
                 {"exerciseId":%d,"reps":8,"weight":0}]
                """.formatted(own, someoneElses)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unknown exercise"));

        assertNoWorkouts(user);
    }

    @Test
    void timesWithAnOffsetAreSavedAsTheSameMomentInUtc() throws Exception {
        String user = "offset-user";

        finish(user, workoutJson(UUID.randomUUID(), "2026-06-01T09:00:00+01:00", "2026-06-01T10:30:00+01:00",
                oneSet(createExercise(user))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.startTime").value("2026-06-01T08:00:00Z"))
            .andExpect(jsonPath("$.endTime").value("2026-06-01T09:30:00Z"));
    }

    @Test
    void aTimeWithoutAnOffsetIsABadRequest() throws Exception {
        String user = "no-offset-user";

        finish(user, workoutJson(UUID.randomUUID(), "2026-06-01T09:00:00", ENDED, oneSet(createExercise(user))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getWorkoutDetailsReturnsNotFoundForOtherUser() throws Exception {
        String body = finish(TEST_USER, workoutJson(UUID.randomUUID(), STARTED, ENDED, oneSet(createExercise(TEST_USER))))
            .andReturn().getResponse().getContentAsString();
        long workoutId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/v1/workouts/{id}", workoutId)
                .with(jwt().jwt(j -> j.subject(OTHER_USER))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Workout not found"));
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/workouts"))
            .andExpect(status().isUnauthorized());
    }

    private ResultActions finish(String userId, String workoutJson) throws Exception {
        return mockMvc.perform(post("/api/v1/workouts")
            .with(jwt().jwt(j -> j.subject(userId)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(workoutJson));
    }

    private static String workoutJson(UUID id, String startedAt, String endedAt, String setsJson) {
        return """
            {"id":"%s","startedAt":"%s","endedAt":"%s","sets":%s}
            """.formatted(id, startedAt, endedAt, setsJson);
    }

    private static String oneSet(long exerciseId) {
        return """
            [{"exerciseId":%d,"reps":8,"weight":0}]
            """.formatted(exerciseId);
    }

    private long createExercise(String userId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Exercise %s"}
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void assertNoWorkouts(String userId) throws Exception {
        mockMvc.perform(get("/api/v1/workouts").with(jwt().jwt(j -> j.subject(userId))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", empty()));
    }
}
