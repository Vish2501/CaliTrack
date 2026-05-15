package com.calitrack.service;

import com.calitrack.dto.CoachRecommendationRequest;
import com.calitrack.dto.CoachRecommendationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class AICoachService {

    private final RestTemplate restTemplate;
    private final String openaiApiKey;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-3.5-turbo";

    public AICoachService(RestTemplate restTemplate,
                          @Value("${openai.api-key}") String openaiApiKey) {
        this.restTemplate = restTemplate;
        this.openaiApiKey = openaiApiKey;
    }

    public CoachRecommendationResponse getRecommendation(CoachRecommendationRequest request) {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI coach is not configured (missing OPENAI_API_KEY)"
            );
        }

        try {
            String prompt = buildPrompt(request);
            String gptResponse = callOpenAIAPI(prompt);
            return parseResponse(gptResponse);
        } catch (HttpClientErrorException e) {
            throw mapOpenAIError(e);
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Failed to reach AI coach service",
                e
            );
        }
    }

    private ResponseStatusException mapOpenAIError(HttpClientErrorException e) {
        String body = e.getResponseBodyAsString();
        if (body.contains("insufficient_quota")) {
            return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI coach is temporarily unavailable (OpenAI quota exceeded). Check billing at platform.openai.com."
            );
        }
        if (e.getStatusCode().value() == 401) {
            return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI coach is not configured (invalid OpenAI API key)"
            );
        }
        return new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "AI coach request failed: " + e.getStatusText()
        );
    }

    private String buildPrompt(CoachRecommendationRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert fitness coach. Based on the user's recent workout history, ");
        prompt.append("provide personalized recommendations.\n\n");
        
        prompt.append("User Profile:\n");
        prompt.append("- Fitness Level: ").append(request.fitnessLevel()).append("\n");
        prompt.append("- Goals: ").append(request.userGoals()).append("\n\n");
        
        prompt.append("Recent Workouts:\n");
        for (CoachRecommendationRequest.WorkoutSummary workout : request.recentWorkouts()) {
            prompt.append("- Date: ").append(workout.date()).append("\n");
            for (CoachRecommendationRequest.WorkoutSummary.ExerciseSummary exercise : workout.exercises()) {
                prompt.append("  * ").append(exercise.name())
                        .append(" (").append(exercise.category()).append(")")
                        .append(": ").append(exercise.totalSets()).append(" sets, ")
                        .append("avg ").append(exercise.averageReps()).append(" reps, ")
                        .append(exercise.averageWeight()).append(" lbs, ")
                        .append("RPE: ").append(exercise.averageRPE()).append("\n");
            }
        }
        
        prompt.append("\nProvide recommendations in this exact format:\n");
        prompt.append("RECOMMENDATION: [2-3 sentence recommendation]\n");
        prompt.append("FOCUS_AREA: [primary area to focus on]\n");
        prompt.append("NEXT_EXERCISE: [one exercise suggestion for next workout]");
        
        return prompt.toString();
    }

    private String callOpenAIAPI(String prompt) {
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
                "content", "You are a professional fitness coach providing personalized workout recommendations."
            ),
            Map.of(
                "role", "user",
                "content", prompt
            )
        ));

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

    private CoachRecommendationResponse parseResponse(String gptResponse) {
        String recommendation = extractSection(gptResponse, "RECOMMENDATION:");
        String focusArea = extractSection(gptResponse, "FOCUS_AREA:");
        String nextExercise = extractSection(gptResponse, "NEXT_EXERCISE:");

        return new CoachRecommendationResponse(
            recommendation.trim(),
            focusArea.trim(),
            nextExercise.trim()
        );
    }

    private String extractSection(String response, String marker) {
        int startIndex = response.indexOf(marker);
        if (startIndex == -1) {
            return "Unable to extract recommendation";
        }
        
        startIndex += marker.length();
        int endIndex = response.indexOf("\n", startIndex);
        
        if (endIndex == -1) {
            endIndex = response.length();
        }
        
        return response.substring(startIndex, endIndex);
    }
}
