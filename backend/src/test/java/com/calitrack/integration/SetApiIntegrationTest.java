package com.calitrack.integration;

import com.calitrack.support.AbstractIntegrationTest;
import com.calitrack.support.FinishedWorkouts;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sets are added only by finishing a workout. These cover reading, correcting and removing them.
class SetApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listCorrectAndDeleteASetOfAFinishedWorkout() throws Exception {
        long workoutId = FinishedWorkouts.finish(mockMvc, TEST_USER);

        String sets = mockMvc.perform(get("/api/v1/workouts/{workoutId}/sets", workoutId)
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].reps").value(8))
            .andReturn().getResponse().getContentAsString();
        long setId = objectMapper.readTree(sets).get(0).get("id").asLong();

        mockMvc.perform(patch("/api/v1/workouts/{workoutId}/sets/{setId}", workoutId, setId)
                .with(jwt().jwt(j -> j.subject(TEST_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reps":10}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reps").value(10));

        mockMvc.perform(delete("/api/v1/workouts/{workoutId}/sets/{setId}", workoutId, setId)
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/workouts/{workoutId}/sets", workoutId)
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listingSetsOfAnUnknownWorkoutIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/workouts/{workoutId}/sets", 9999L)
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Workout not found"));
    }
}
