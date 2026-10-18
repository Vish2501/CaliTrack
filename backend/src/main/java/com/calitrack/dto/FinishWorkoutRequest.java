package com.calitrack.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// A whole workout, sent once when the user finishes it. The phone generates the ID, so sending
// the same workout again returns the one already saved instead of saving a copy.
public record FinishWorkoutRequest(
    @NotNull
    UUID id,

    @NotNull
    Instant startedAt,

    @NotNull
    Instant endedAt,

    @Size(max = 2000)
    String notes,

    // In the order they were done.
    @NotEmpty
    List<@NotNull @Valid FinishedSet> sets
) {
    public record FinishedSet(
        @NotNull
        Long exerciseId,

        @NotNull
        @Positive
        Integer reps,

        @NotNull
        @DecimalMin("0.0")
        Double weight
    ) {}
}
