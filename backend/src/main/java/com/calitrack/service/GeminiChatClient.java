package com.calitrack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Calls Google Gemini through its OpenAI-compatible chat completions endpoint, which the free tier
 * of the Gemini API supports. Throws Spring's RestClientException family on HTTP or network
 * failure; callers decide how to map or retry them.
 */
@Component
@Conditional(CoachModelSelection.GeminiModel.class)
public class GeminiChatClient implements CoachModelClient {

    private static final String SYSTEM_PROMPT =
            "You are a professional fitness coach providing personalized workout recommendations.";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String chatCompletionsUrl;

    public GeminiChatClient(RestTemplate restTemplate,
                            @Value("${gemini.api-key}") String apiKey,
                            @Value("${gemini.model}") String model,
                            @Value("${gemini.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.model = model;
        this.chatCompletionsUrl = baseUrl.replaceAll("/+$", "") + "/chat/completions";
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String complete(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // No max_tokens: on Gemini, thinking tokens can count towards it and truncate the reply.
        // The prompt itself keeps the answer short.
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "temperature", 0.7,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", prompt)));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                chatCompletionsUrl, new HttpEntity<>(requestBody, headers), Map.class);

        if (response == null || !response.containsKey("choices")) {
            throw new RuntimeException("Invalid response from Gemini API");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices.isEmpty()) {
            throw new RuntimeException("No choices returned from Gemini API");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
