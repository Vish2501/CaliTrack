package com.calitrack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the OpenAI chat completions endpoint. Throws Spring's RestClientException
 * family on HTTP or network failure; callers decide how to map or retry them.
 */
@Component
public class OpenAIChatClient {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-3.5-turbo";

    private final RestTemplate restTemplate;
    private final String openaiApiKey;

    public OpenAIChatClient(RestTemplate restTemplate, @Value("${openai.api-key}") String openaiApiKey) {
        this.restTemplate = restTemplate;
        this.openaiApiKey = openaiApiKey;
    }

    public boolean isConfigured() {
        return openaiApiKey != null && !openaiApiKey.isBlank();
    }

    public String complete(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + openaiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 500);
        requestBody.put("messages", List.of(
                Map.of(
                        "role", "system",
                        "content",
                        "You are a professional fitness coach providing personalized workout recommendations."),
                Map.of(
                        "role", "user",
                        "content", prompt)));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(OPENAI_API_URL, entity, Map.class);

        if (response == null || !response.containsKey("choices")) {
            throw new RuntimeException("Invalid response from OpenAI API");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices.isEmpty()) {
            throw new RuntimeException("No choices returned from OpenAI API");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
