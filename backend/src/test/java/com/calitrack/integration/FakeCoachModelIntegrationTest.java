package com.calitrack.integration;

import com.calitrack.service.CoachModelClient;
import com.calitrack.service.FakeCoachModelClient;
import com.calitrack.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
    "calitrack.coach.fake-model=true",
    "calitrack.coach.fake-model-delay-ms=0"
})
class FakeCoachModelIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CoachModelClient coachModelClient;

    @Test
    void fakeModelReplacesOpenAIAndServesTheCoachEndpoint() throws Exception {
        assertThat(coachModelClient).isInstanceOf(FakeCoachModelClient.class);

        mockMvc.perform(get("/api/v1/coach/recommend").with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendation").value(startsWith("[Demo reply, no OpenAI call]")))
            .andExpect(jsonPath("$.suggestedNextExercise").value("Push-Up"));
    }
}
