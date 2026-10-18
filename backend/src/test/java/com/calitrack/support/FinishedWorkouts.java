package com.calitrack.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Finishes a workout through the API the way the phone does, for tests that need one to exist.
public final class FinishedWorkouts {

    private static final ObjectMapper JSON = new ObjectMapper();

    private FinishedWorkouts() {
    }

    // Creates an exercise, finishes a one-set workout with it, and returns the workout's ID.
    public static long finish(MockMvc mockMvc, String userId) throws Exception {
        String exercise = mockMvc.perform(post("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Pull-Up %s"}
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long exerciseId = JSON.readTree(exercise).get("id").asLong();

        Instant endedAt = Instant.now();
        String workout = mockMvc.perform(post("/api/v1/workouts")
                .with(jwt().jwt(j -> j.subject(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"id":"%s","startedAt":"%s","endedAt":"%s",
                     "sets":[{"exerciseId":%d,"reps":8,"weight":0}]}
                    """.formatted(UUID.randomUUID(), endedAt.minus(Duration.ofHours(1)), endedAt, exerciseId)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JSON.readTree(workout).get("id").asLong();
    }
}
