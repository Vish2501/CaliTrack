package com.calitrack.dto;

import java.time.LocalDateTime;

public record CreateSetRequest(
    Long exerciseId,
    Integer reps,
    Double weight,
    Double rpe,
    LocalDateTime timestamp
) {}

