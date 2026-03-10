package com.calitrack.dto;

import java.time.LocalDateTime;
import java.util.List;

public record WorkoutDetailsResponse(
    Long id,
    String userId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String notes,
    List<SetResponse> sets
) {}

