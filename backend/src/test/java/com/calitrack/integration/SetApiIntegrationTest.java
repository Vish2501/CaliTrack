package com.calitrack.integration;

import com.calitrack.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SetApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long workoutId;
    private long exerciseId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult workoutResult = mockMvc.perform(post("/api/v1/workouts")
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isCreated())
            .andReturn();

        workoutId = objectMapper.readTree(workoutResult.getResponse().getContentAsString())
            .get("id")
            .asLong();

        MvcResult exerciseResult = mockMvc.perform(post("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(TEST_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Push-Up","category":"Chest"}
                    """))
            .andExpect(status().isCreated())
            .andReturn();

        exerciseId = objectMapper.readTree(exerciseResult.getResponse().getContentAsString())
            .get("id")
            .asLong();
    }

    @Test
    void addAndListSets() throws Exception {
        mockMvc.perform(post("/api/v1/workouts/{workoutId}/sets", workoutId)
                .with(jwt().jwt(j -> j.subject(TEST_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"exerciseId":%d,"reps":10,"weight":0,"rpe":7}
                    """.formatted(exerciseId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.exerciseName").value("Push-Up"))
            .andExpect(jsonPath("$.reps").value(10));

        mockMvc.perform(get("/api/v1/workouts/{workoutId}/sets", workoutId)
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].reps").value(10));
    }

    @Test
    void addSetReturnsNotFoundForUnknownWorkout() throws Exception {
        mockMvc.perform(post("/api/v1/workouts/{workoutId}/sets", 9999L)
                .with(jwt().jwt(j -> j.subject(TEST_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"exerciseId":%d,"reps":8,"weight":0,"rpe":6}
                    """.formatted(exerciseId)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Workout not found"));
    }
}
