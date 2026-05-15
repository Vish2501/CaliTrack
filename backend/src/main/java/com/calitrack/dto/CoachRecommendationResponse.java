package com.calitrack.dto;

public record CoachRecommendationResponse(
    String recommendation,
    String focusArea,
    String suggestedNextExercise
) {}
