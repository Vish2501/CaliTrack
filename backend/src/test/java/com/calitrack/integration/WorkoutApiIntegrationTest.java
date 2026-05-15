package com.calitrack.integration;

import com.calitrack.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkoutApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void startListAndFinishWorkout() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/api/v1/workouts")
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value(TEST_USER))
            .andExpect(jsonPath("$.endTime").isEmpty())
            .andReturn();

        long workoutId = objectMapper.readTree(startResult.getResponse().getContentAsString())
            .get("id")
            .asLong();

        mockMvc.perform(get("/api/v1/workouts")
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(workoutId));

        mockMvc.perform(patch("/api/v1/workouts/{id}/finish", workoutId)
                .with(jwt().jwt(j -> j.subject(TEST_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.endTime").isNotEmpty());
    }

    @Test
    void getWorkoutDetailsReturnsNotFoundForOtherUser() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/api/v1/workouts")
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode workout = objectMapper.readTree(startResult.getResponse().getContentAsString());
        long workoutId = workout.get("id").asLong();

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
}
