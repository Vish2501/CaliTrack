package com.calitrack.dto;

import java.util.List;

public record CoachRecommendationRequest(
        List<WorkoutSummary> recentWorkouts,
        String userGoals,
        String fitnessLevel) {
    public record WorkoutSummary(
            String date,
            List<ExerciseSummary> exercises) {
        public record ExerciseSummary(
                String name,
                String category,
                int totalSets,
                double averageReps,
                double averageWeight,
                double averageRPE) {
        }
    }
}
