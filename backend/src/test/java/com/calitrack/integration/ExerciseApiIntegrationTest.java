package com.calitrack.integration;

import com.calitrack.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createListAndDeleteExercise() throws Exception {
        mockMvc.perform(post("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(TEST_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Pull-Up","category":"Back"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Pull-Up"))
            .andExpect(jsonPath("$.userId").value(TEST_USER));

        mockMvc.perform(get("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Pull-Up"));

        mockMvc.perform(delete("/api/v1/exercises/1")
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void deleteExerciseReturnsForbiddenForOtherUser() throws Exception {
        mockMvc.perform(post("/api/v1/exercises")
                .with(jwt().jwt(j -> j.subject(TEST_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Dip","category":"Triceps"}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/exercises/1")
                .with(jwt().jwt(j -> j.subject(OTHER_USER))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Forbidden"));
    }
}
