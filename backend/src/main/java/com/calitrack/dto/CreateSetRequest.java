package com.calitrack.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record CreateSetRequest(
    @NotNull
    @Positive
    Long exerciseId,

    @NotNull
    @Positive
    Integer reps,

    @NotNull
    @DecimalMin("0.0")
    Double weight,

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    Double rpe,

    LocalDateTime timestamp
) {}
