package com.calitrack.dto;

import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;

public record FinishWorkoutRequest(
    @PastOrPresent
    LocalDateTime endedAt
) {}
