package com.calitrack.dto;

// One set from the last time the user did an exercise, used to suggest values for today's sets.
public record PreviousSetResponse(
    Integer reps,
    Double weight
) {}
