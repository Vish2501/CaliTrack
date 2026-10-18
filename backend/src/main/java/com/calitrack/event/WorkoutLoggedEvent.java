package com.calitrack.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Raised when a workout is finished. Published inside the app as a Spring event and, when the
 * async coach is enabled, sent to the workout-logged Kafka topic as JSON keyed by user ID.
 */
public record WorkoutLoggedEvent(
    Long workoutId,
    String userId,
    // Spring Kafka's JsonSerializer would otherwise write the time as a number.
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant finishedAt
) {
}
