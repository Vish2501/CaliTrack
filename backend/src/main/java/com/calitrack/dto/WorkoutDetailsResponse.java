package com.calitrack.dto;

import java.time.Instant;
import java.util.List;

public record WorkoutDetailsResponse(
    Long id,
    String userId,
    Instant startTime,
    Instant endTime,
    String notes,
    List<SetResponse> sets
) {}

