package com.calitrack.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiChatClientTest {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/";

    @Test
    void sendsAnOpenAiCompatibleChatRequestToGeminiAndReturnsTheReply() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        GeminiChatClient client = new GeminiChatClient(restTemplate, "test-key", "gemini-3.5-flash-lite", BASE_URL);

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer test-key"))
            .andExpect(jsonPath("$.model").value("gemini-3.5-flash-lite"))
            .andExpect(jsonPath("$.messages[0].role").value("system"))
            .andExpect(jsonPath("$.messages[1].role").value("user"))
            .andExpect(jsonPath("$.messages[1].content").value("How did I do?"))
            .andExpect(jsonPath("$.max_tokens").doesNotExist())
            .andRespond(withSuccess("""
                {"choices":[{"index":0,"message":{"role":"assistant","content":"FOCUS_AREA: Back"}}]}
                """, MediaType.APPLICATION_JSON));

        assertThat(client.complete("How did I do?")).isEqualTo("FOCUS_AREA: Back");
        server.verify();
    }

    @Test
    void isNotConfiguredWithoutAKey() {
        assertThat(new GeminiChatClient(new RestTemplate(), "", "gemini-3.5-flash-lite", BASE_URL).isConfigured())
            .isFalse();
    }
}
