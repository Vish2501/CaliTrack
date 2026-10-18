package com.calitrack.controller;

import com.calitrack.dto.FinishWorkoutRequest;
import com.calitrack.dto.UpdateWorkoutRequest;
import com.calitrack.dto.WorkoutDetailsResponse;
import com.calitrack.entity.Workout;
import com.calitrack.service.CurrentUserService;
import com.calitrack.service.WorkoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // Saves a finished workout. Safe to retry: sending the same workout ID again returns the
    // workout already saved (200) instead of saving a copy (201).
    @PostMapping
    public ResponseEntity<WorkoutDetailsResponse> finishWorkout(
        @Valid @RequestBody FinishWorkoutRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = currentUserService.getUserId(jwt);
        return workoutService.findFinishedWorkout(userId, request.id())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.CREATED)
                .body(workoutService.finishWorkout(userId, request)));
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
}
