package com.calitrack.dto;

import java.time.LocalDateTime;

public record SetResponse(
    Long id,
    Long workoutId,
    Long exerciseId,
    String exerciseName,
    Integer reps,
    Double weight,
    Double rpe,
    LocalDateTime timestamp
) {}

