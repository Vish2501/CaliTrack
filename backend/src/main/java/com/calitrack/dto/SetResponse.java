package com.calitrack.dto;

public record SetResponse(
    Long id,
    Long workoutId,
    Long exerciseId,
    String exerciseName,
    Integer reps,
    Double weight,
    Double rpe
) {}

