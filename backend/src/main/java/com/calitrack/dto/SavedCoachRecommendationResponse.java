package com.calitrack.dto;

import java.time.LocalDateTime;

public record SavedCoachRecommendationResponse(
        Long workoutId,
        String recommendation,
        String focusArea,
        String suggestedNextExercise,
        LocalDateTime createdAt) {
}
