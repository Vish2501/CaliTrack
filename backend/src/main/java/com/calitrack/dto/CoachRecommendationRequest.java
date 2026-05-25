package com.calitrack.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CoachRecommendationRequest(
        @NotNull
        @Size(max = 10)
        List<@Valid WorkoutSummary> recentWorkouts,

        @NotBlank
        @Size(max = 200)
        String userGoals,

        @NotBlank
        @Size(max = 50)
        String fitnessLevel) {
    public record WorkoutSummary(
            @NotBlank
            @Size(max = 20)
            String date,

            @NotNull
            @Size(max = 50)
            List<@Valid ExerciseSummary> exercises) {
        public record ExerciseSummary(
                @NotBlank
                @Size(max = 100)
                String name,

                @Size(max = 100)
                String category,

                @PositiveOrZero
                int totalSets,

                @PositiveOrZero
                double averageReps,

                @PositiveOrZero
                double averageWeight,

                @DecimalMin("0.0")
                @DecimalMax("10.0")
                double averageRPE) {
        }
    }
}
