package com.calitrack.dto;

import java.time.Instant;

public record SetResponse(
    Long id,
    Long workoutId,
    Long exerciseId,
    String exerciseName,
    Integer reps,
    Double weight,
    Double rpe,
    Instant timestamp
) {}

