package com.calitrack.controller;

import com.calitrack.dto.CreateExerciseRequest;
import com.calitrack.dto.PreviousSetResponse;
import com.calitrack.entity.Exercise;
import com.calitrack.service.CurrentUserService;
import com.calitrack.service.ExerciseService;
import com.calitrack.service.SetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final SetService setService;
    private final CurrentUserService currentUserService;

    public ExerciseController(ExerciseService exerciseService, SetService setService,
                              CurrentUserService currentUserService) {
        this.exerciseService = exerciseService;
        this.setService = setService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<Exercise> getExercises(@AuthenticationPrincipal Jwt jwt) {
        String userId = currentUserService.getUserId(jwt);
        return exerciseService.getExercisesForUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Exercise createExercise(
            @Valid @RequestBody CreateExerciseRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return exerciseService.createExercise(request, currentUserService.getUserId(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExercise(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        String userId = currentUserService.getUserId(jwt);
        exerciseService.deleteExercise(id, userId);
    }

    @GetMapping("/{id}/previous-sets")
    public List<PreviousSetResponse> getPreviousSets(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return setService.getPreviousSets(id, currentUserService.getUserId(jwt));
    }
}
