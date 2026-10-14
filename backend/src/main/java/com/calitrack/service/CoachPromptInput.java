package com.calitrack.service;

import java.util.List;

/**
 * What the coach prompt is built from: the user's goals and level, and a summary of their recent
 * workouts. Built on the server from the user's own data, never taken from the client.
 */
record CoachPromptInput(
        List<WorkoutSummary> recentWorkouts,
        String userGoals,
        String fitnessLevel) {

    record WorkoutSummary(String date, List<ExerciseSummary> exercises) {
    }

    record ExerciseSummary(
            String name,
            String category,
            int totalSets,
            double averageReps,
            double averageWeight,
            double averageRPE) {
    }
}
