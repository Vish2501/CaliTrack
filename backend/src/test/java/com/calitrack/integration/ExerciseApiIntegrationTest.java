package com.calitrack.integration;

import com.calitrack.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndListExercise() throws Exception {
        String user = "create-user";
        mockMvc.perform(post("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Pull-Up","category":"Back"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Pull-Up"))
            .andExpect(jsonPath("$.userId").value(user));

        mockMvc.perform(get("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(user))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", contains("Pull-Up")));
    }

    @Test
    void deletingArchivesTheExerciseAndKeepsItsSets() throws Exception {
        String user = "archive-user";
        long exerciseId = createExercise(user, "Pull-Up");
        long workoutId = startWorkout(user);
        mockMvc.perform(post("/api/v1/workouts/{workoutId}/sets", workoutId)
                .with(jwt().jwt(j -> j.subject(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"exerciseId":%d,"reps":8,"weight":0}
                    """.formatted(exerciseId)))
            .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/exercises/{id}", exerciseId)
                .with(jwt().jwt(j -> j.subject(user))))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(user))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", empty()));

        mockMvc.perform(get("/api/v1/workouts/{id}", workoutId)
                .with(jwt().jwt(j -> j.subject(user))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sets[0].exerciseId").value(exerciseId))
            .andExpect(jsonPath("$.sets[0].exerciseName").value("Pull-Up"))
            .andExpect(jsonPath("$.sets[0].reps").value(8));
    }

    @Test
    void creatingAnArchivedExercisesNameRestoresIt() throws Exception {
        String user = "restore-user";
        long exerciseId = createExercise(user, "Pull-Up");
        mockMvc.perform(delete("/api/v1/exercises/{id}", exerciseId)
                .with(jwt().jwt(j -> j.subject(user))))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":" pull-up "}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(exerciseId))
            .andExpect(jsonPath("$.name").value("Pull-Up"));

        mockMvc.perform(get("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(user))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id", contains((int) exerciseId)));
    }

    @Test
    void creatingAnActiveExercisesNameIsAConflict() throws Exception {
        String user = "duplicate-user";
        createExercise(user, "Dip");

        mockMvc.perform(post("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"DIP"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("An exercise with this name already exists"));
    }

    @Test
    void deletingAnotherUsersExerciseIsNotFoundAndLeavesItAlone() throws Exception {
        long exerciseId = createExercise(TEST_USER, "Dip");

        mockMvc.perform(delete("/api/v1/exercises/{id}", exerciseId)
                .with(jwt().jwt(j -> j.subject(OTHER_USER))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Exercise not found"));

        mockMvc.perform(get("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(jsonPath("$[*].name", contains("Dip")));
    }

    private long createExercise(String userId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"%s"}
                    """.formatted(name)))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long startWorkout(String userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/workouts")
                .with(jwt().jwt(j -> j.subject(userId))))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
