package com.calitrack.dto;

import jakarta.validation.constraints.PastOrPresent;

import java.time.Instant;

public record FinishWorkoutRequest(
    @PastOrPresent
    Instant endedAt
) {}
