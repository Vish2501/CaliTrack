package com.calitrack.controller;

import com.calitrack.dto.FinishWorkoutRequest;
import com.calitrack.dto.UpdateWorkoutRequest;
import com.calitrack.dto.WorkoutDetailsResponse;
import com.calitrack.entity.Workout;
import com.calitrack.service.CurrentUserService;
import com.calitrack.service.WorkoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workouts")
public class WorkoutController {

    private final WorkoutService workoutService;
    private final CurrentUserService currentUserService;

    public WorkoutController(WorkoutService workoutService, CurrentUserService currentUserService) {
        this.workoutService = workoutService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Workout startWorkout(@AuthenticationPrincipal Jwt jwt) {
        return workoutService.startWorkout(currentUserService.getUserId(jwt));
    }

    @GetMapping
    public List<Workout> getWorkouts(@AuthenticationPrincipal Jwt jwt) {
        return workoutService.getWorkoutsForUser(currentUserService.getUserId(jwt));
    }

    @GetMapping("/{workoutId}")
    public WorkoutDetailsResponse getWorkoutById(
        @PathVariable Long workoutId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workoutService.getWorkoutDetails(workoutId, currentUserService.getUserId(jwt));
    }

    @PatchMapping("/{workoutId}")
    public Workout updateWorkout(
        @PathVariable Long workoutId,
        @Valid @RequestBody UpdateWorkoutRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workoutService.updateWorkout(workoutId, currentUserService.getUserId(jwt), request.notes());
    }

    @PatchMapping("/{workoutId}/finish")
    public Workout finishWorkout(
        @PathVariable Long workoutId,
        @Valid @RequestBody(required = false) FinishWorkoutRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workoutService.finishWorkout(
            workoutId,
            currentUserService.getUserId(jwt),
            request != null ? request.endedAt() : null
        );
    }
}
