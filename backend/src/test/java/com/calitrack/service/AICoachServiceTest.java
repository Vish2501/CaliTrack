package com.calitrack.service;

import com.calitrack.dto.CoachRecommendationRequest;
import com.calitrack.dto.CoachRecommendationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AICoachServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private AICoachService aiCoachService;

    @BeforeEach
    void setUp() {
        aiCoachService = new AICoachService(restTemplate, "test-key");
    }

    @Test
    void getRecommendationThrowsWhenApiKeyMissing() {
        aiCoachService = new AICoachService(restTemplate, "");

        CoachRecommendationRequest request = new CoachRecommendationRequest(
            List.of(),
            "strength",
            "intermediate"
        );

        assertThatThrownBy(() -> aiCoachService.getRecommendation(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("missing OPENAI_API_KEY");
    }

    @Test
    void getRecommendationParsesOpenAiResponse() {
        Map<String, Object> openAiResponse = Map.of(
            "choices", List.of(
                Map.of(
                    "message", Map.of(
                        "content", """
                            RECOMMENDATION: Add more pulling volume this week.
                            FOCUS_AREA: Back and biceps
                            NEXT_EXERCISE: Chin-Up
                            """
                    )
                )
            )
        );

        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(openAiResponse);

        CoachRecommendationResponse response = aiCoachService.getRecommendation(
            new CoachRecommendationRequest(List.of(), "strength", "intermediate")
        );

        assertThat(response.recommendation()).contains("pulling volume");
        assertThat(response.focusArea()).isEqualTo("Back and biceps");
        assertThat(response.suggestedNextExercise()).isEqualTo("Chin-Up");
    }
}
