package com.calitrack.controller;

import com.calitrack.dto.FinishWorkoutRequest;
import com.calitrack.dto.UpdateWorkoutRequest;
import com.calitrack.dto.WorkoutDetailsResponse;
import com.calitrack.entity.Workout;
import com.calitrack.service.WorkoutService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @PostMapping
    public Workout startWorkout(@AuthenticationPrincipal Jwt jwt) {
        return workoutService.startWorkout(jwt.getSubject());
    }

    @GetMapping
    public List<Workout> getWorkouts(@AuthenticationPrincipal Jwt jwt) {
        return workoutService.getWorkoutsForUser(jwt.getSubject());
    }

    @GetMapping("/{workoutId}")
    public WorkoutDetailsResponse getWorkoutById(
        @PathVariable Long workoutId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workoutService.getWorkoutDetails(workoutId, jwt.getSubject());
    }

    @PatchMapping("/{workoutId}")
    public Workout updateWorkout(
        @PathVariable Long workoutId,
        @RequestBody UpdateWorkoutRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workoutService.updateWorkout(workoutId, jwt.getSubject(), request.notes());
    }

    @PatchMapping("/{workoutId}/finish")
    public Workout finishWorkout(
        @PathVariable Long workoutId,
        @RequestBody(required = false) FinishWorkoutRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workoutService.finishWorkout(
            workoutId,
            jwt.getSubject(),
            request != null ? request.endedAt() : null
        );
    }
}
