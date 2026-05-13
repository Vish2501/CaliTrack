package com.calitrack.dto;

import java.time.LocalDateTime;

public record UpdateSetRequest(
    Integer reps,
    Double weight,
    Double rpe,
    LocalDateTime timestamp
) {}

