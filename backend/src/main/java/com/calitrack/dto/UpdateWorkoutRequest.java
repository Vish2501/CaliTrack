package com.calitrack.dto;

import jakarta.validation.constraints.Size;

public record UpdateWorkoutRequest(
    @Size(max = 2000)
    String notes
) {}
