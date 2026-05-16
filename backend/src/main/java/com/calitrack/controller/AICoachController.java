package com.calitrack.controller;

import com.calitrack.dto.CoachRecommendationRequest;
import com.calitrack.dto.CoachRecommendationResponse;
import com.calitrack.entity.Workout;
import com.calitrack.entity.Set;
import com.calitrack.entity.Exercise;
import com.calitrack.service.AICoachService;
import com.calitrack.service.WorkoutService;
import com.calitrack.repository.SetRepository;
import com.calitrack.repository.ExerciseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/coach")
public class AICoachController {

    private final AICoachService aiCoachService;
    private final WorkoutService workoutService;
    private final SetRepository setRepository;
    private final ExerciseRepository exerciseRepository;

    public AICoachController(AICoachService aiCoachService,
            WorkoutService workoutService,
            SetRepository setRepository,
            ExerciseRepository exerciseRepository) {
        this.aiCoachService = aiCoachService;
        this.workoutService = workoutService;
        this.setRepository = setRepository;
        this.exerciseRepository = exerciseRepository;
    }

    /**
     * Get AI-powered coaching recommendations based on recent workout history
     * 
     * @param request The recommendation request with workout history and user goals
     * @param jwt     The authenticated user's JWT token
     * @return AI-generated coaching recommendation
     */
    @PostMapping("/recommend")
    public ResponseEntity<CoachRecommendationResponse> getRecommendation(
            @RequestBody CoachRecommendationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        CoachRecommendationResponse recommendation = aiCoachService.getRecommendation(request);
        return ResponseEntity.ok(recommendation);
    }

    /**
     * Get AI coaching recommendation by automatically fetching recent workouts
     * 
     * @param numWorkouts  Number of recent completed workouts to analyze (default:
     *                     5)
     * @param userGoals    User's fitness goals (e.g., "strength", "endurance",
     *                     "hypertrophy")
     * @param fitnessLevel User's fitness level (e.g., "beginner", "intermediate",
     *                     "advanced")
     * @param jwt          The authenticated user's JWT token
     * @return AI-generated coaching recommendation
     */
    @GetMapping("/recommend")
    public ResponseEntity<CoachRecommendationResponse> getAutoRecommendation(
            @RequestParam(defaultValue = "5") int numWorkouts,
            @RequestParam(defaultValue = "general fitness") String userGoals,
            @RequestParam(defaultValue = "intermediate") String fitnessLevel,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();

        List<Workout> recentWorkouts = workoutService.getWorkoutsForUser(userId)
                .stream()
                .filter(w -> w.getEndTime() != null)
                .sorted((w1, w2) -> w2.getEndTime().compareTo(w1.getEndTime()))
                .limit(numWorkouts)
                .toList();

        List<CoachRecommendationRequest.WorkoutSummary> workoutSummaries = recentWorkouts.stream()
                .map(this::buildWorkoutSummary)
                .toList();

        CoachRecommendationRequest request = new CoachRecommendationRequest(
                workoutSummaries,
                userGoals,
                fitnessLevel);

        CoachRecommendationResponse recommendation = aiCoachService.getRecommendation(request);
        return ResponseEntity.ok(recommendation);
    }

    private CoachRecommendationRequest.WorkoutSummary buildWorkoutSummary(Workout workout) {
        List<Set> sets = setRepository.findByWorkoutId(workout.getId());
        Map<Long, Exercise> exerciseMap = exerciseRepository.findAllById(
                sets.stream().map(Set::getExerciseId).distinct().toList()).stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e, (a, b) -> a));

        // Group sets by exercise
        Map<Long, List<Set>> setsByExercise = sets.stream()
                .collect(Collectors.groupingBy(Set::getExerciseId));

        List<CoachRecommendationRequest.WorkoutSummary.ExerciseSummary> exerciseSummaries = setsByExercise.entrySet()
                .stream()
                .filter(entry -> exerciseMap.containsKey(entry.getKey()))
                .map(entry -> {
                    Exercise exercise = exerciseMap.get(entry.getKey());
                    List<Set> exerciseSets = entry.getValue();

                    double avgReps = exerciseSets.stream()
                            .mapToInt(s -> s.getReps() != null ? s.getReps() : 0)
                            .average()
                            .orElse(0);

                    double avgWeight = exerciseSets.stream()
                            .mapToDouble(s -> s.getWeight() != null ? s.getWeight() : 0)
                            .average()
                            .orElse(0);

                    double avgRPE = exerciseSets.stream()
                            .mapToDouble(s -> s.getRpe() != null ? s.getRpe() : 0)
                            .average()
                            .orElse(0);

                    return new CoachRecommendationRequest.WorkoutSummary.ExerciseSummary(
                            exercise.getName(),
                            exercise.getCategory(),
                            exerciseSets.size(),
                            avgReps,
                            avgWeight,
                            avgRPE);
                })
                .toList();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateStr = workout.getStartTime().format(formatter);

        return new CoachRecommendationRequest.WorkoutSummary(dateStr, exerciseSummaries);
    }
}
