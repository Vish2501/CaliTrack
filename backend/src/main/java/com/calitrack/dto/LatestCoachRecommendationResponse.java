package com.calitrack.dto;

/**
 * The user's most recent saved recommendation (null if none yet), and whether one is still being
 * generated for a workout they just finished.
 */
public record LatestCoachRecommendationResponse(
        SavedCoachRecommendationResponse recommendation,
        boolean pending) {
}
