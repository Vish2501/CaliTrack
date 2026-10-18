package com.calitrack.integration;

import com.calitrack.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GET /api/v1/exercises/{id}/previous-sets: the sets from the last time the user did an exercise.
class PreviousSetsApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsTheSetsFromTheMostRecentWorkoutWithThatExerciseInOrder() throws Exception {
        String user = "recent-user";
        long squat = createExercise(user);
        long plank = createExercise(user);

        // Last week: an older Squat session.
        finishWorkout(user, "2026-06-01T09:00:00Z", "2026-06-01T10:00:00Z",
            set(squat, 8, 20), set(squat, 8, 20));
        // This week: the latest Squat session, a pyramid, with Plank sets in between.
        finishWorkout(user, "2026-06-08T09:00:00Z", "2026-06-08T10:00:00Z",
            set(squat, 8, 20), set(plank, 1, 0), set(squat, 6, 22), set(squat, 4, 24));
        // Yesterday: a workout without Squat, which mustn't count.
        finishWorkout(user, "2026-06-10T09:00:00Z", "2026-06-10T10:00:00Z",
            set(plank, 1, 0));

        mockMvc.perform(get("/api/v1/exercises/{id}/previous-sets", squat)
                .with(jwt().jwt(j -> j.subject(user))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].reps", contains(8, 6, 4)))
            .andExpect(jsonPath("$[*].weight", contains(20.0, 22.0, 24.0)));
    }

    @Test
    void returnsNothingForAnExerciseNeverDone() throws Exception {
        String user = "new-user";
        long squat = createExercise(user);

        mockMvc.perform(get("/api/v1/exercises/{id}/previous-sets", squat)
                .with(jwt().jwt(j -> j.subject(user))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", empty()));
    }

    @Test
    void anotherUsersExerciseIsNotFound() throws Exception {
        long theirs = createExercise(OTHER_USER);
        finishWorkout(OTHER_USER, "2026-06-01T09:00:00Z", "2026-06-01T10:00:00Z", set(theirs, 8, 20));

        mockMvc.perform(get("/api/v1/exercises/{id}/previous-sets", theirs)
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Exercise not found"));
    }

    @Test
    void anUnknownExerciseIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/exercises/{id}/previous-sets", 999999L)
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isNotFound());
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

    private void finishWorkout(String userId, String startedAt, String endedAt, String... sets) throws Exception {
        mockMvc.perform(post("/api/v1/workouts")
                .with(jwt().jwt(j -> j.subject(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"id":"%s","startedAt":"%s","endedAt":"%s","sets":[%s]}
                    """.formatted(UUID.randomUUID(), startedAt, endedAt, String.join(",", sets))))
            .andExpect(status().isCreated());
    }

    private static String set(long exerciseId, int reps, double weight) {
        return """
            {"exerciseId":%d,"reps":%d,"weight":%s}""".formatted(exerciseId, reps, weight);
    }
}
