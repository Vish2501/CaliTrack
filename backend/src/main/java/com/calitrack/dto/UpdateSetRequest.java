package com.calitrack.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record UpdateSetRequest(
    @Positive
    Integer reps,

    @DecimalMin("0.0")
    Double weight,

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    Double rpe,

    Instant timestamp
) {}
