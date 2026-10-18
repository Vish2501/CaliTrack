package com.calitrack.integration;

import com.calitrack.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Client mistakes must come back as 4xx with the usual error body, not as a 500.
class ErrorStatusIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void removedCoachPostEndpointIsMethodNotAllowed() throws Exception {
        // POST /coach/recommend used to accept client-built workout data for the AI prompt.
        mockMvc.perform(post("/api/v1/coach/recommend")
                .with(jwt().jwt(j -> j.subject(TEST_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405))
            .andExpect(jsonPath("$.path").value("/api/v1/coach/recommend"));
    }

    @Test
    void unknownPathIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist").with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/workouts")
                .with(jwt().jwt(j -> j.subject(TEST_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void nonNumericIdIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/workouts/abc").with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("workoutId has an invalid value"));
    }
}
