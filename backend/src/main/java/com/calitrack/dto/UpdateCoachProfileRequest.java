package com.calitrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCoachProfileRequest(
    @NotBlank
    @Size(max = 200)
    String goals,

    @NotBlank
    @Size(max = 50)
    String fitnessLevel
) {}
