package com.calitrack.dto;

import java.time.Instant;

public record SavedCoachRecommendationResponse(
        Long workoutId,
        String recommendation,
        String focusArea,
        String suggestedNextExercise,
        Instant createdAt) {
}
