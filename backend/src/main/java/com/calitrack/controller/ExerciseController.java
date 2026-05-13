package com.calitrack.controller;

import com.calitrack.entity.Exercise;
import com.calitrack.service.ExerciseService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public List<Exercise> getExercises(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return exerciseService.getExercisesForUser(userId);
    }

    @PostMapping
    public Exercise createExercise(@RequestBody Exercise exercise, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        exercise.setUserId(userId);
        return exerciseService.createExercise(exercise);
    }

    @DeleteMapping("/{id}")
    public void deleteExercise(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        exerciseService.deleteExercise(id, userId);
    }
}
