package com.calitrack.service;

import com.calitrack.dto.CoachRecommendationRequest;
import com.calitrack.dto.CoachRecommendationResponse;
import com.calitrack.entity.Exercise;
import com.calitrack.entity.Set;
import com.calitrack.entity.Workout;
import com.calitrack.repository.ExerciseRepository;
import com.calitrack.repository.SetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AICoachService {

    private final CoachModelClient coachModelClient;
    private final WorkoutService workoutService;
    private final SetRepository setRepository;
    private final ExerciseRepository exerciseRepository;

    private static final DateTimeFormatter WORKOUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public AICoachService(CoachModelClient coachModelClient,
            WorkoutService workoutService,
            SetRepository setRepository,
            ExerciseRepository exerciseRepository) {
        this.coachModelClient = coachModelClient;
        this.workoutService = workoutService;
        this.setRepository = setRepository;
        this.exerciseRepository = exerciseRepository;
    }

    public CoachRecommendationResponse getAutoRecommendation(
            String userId,
            int numWorkouts,
            String userGoals,
            String fitnessLevel) {
        List<CoachRecommendationRequest.WorkoutSummary> workoutSummaries = workoutService.getWorkoutsForUser(userId)
                .stream()
                .filter(workout -> workout.getEndTime() != null)
                .sorted(Comparator.comparing(Workout::getEndTime).reversed())
                .limit(numWorkouts)
                .map(this::buildWorkoutSummary)
                .toList();

        CoachRecommendationRequest request = new CoachRecommendationRequest(
                workoutSummaries,
                userGoals,
                fitnessLevel);

        return getRecommendation(request);
    }

    public CoachRecommendationResponse getRecommendation(CoachRecommendationRequest request) {
        if (!coachModelClient.isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI coach is not configured (missing GEMINI_API_KEY)");
        }

        try {
            String prompt = buildPrompt(request);
            String modelReply = coachModelClient.complete(prompt);
            return parseResponse(modelReply);
        } catch (HttpClientErrorException e) {
            throw mapModelError(e);
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to reach AI coach service",
                    e);
        }
    }

    private CoachRecommendationRequest.WorkoutSummary buildWorkoutSummary(Workout workout) {
        List<Set> sets = setRepository.findByWorkoutId(workout.getId());
        Map<Long, Exercise> exerciseMap = exerciseRepository.findAllById(
                sets.stream().map(Set::getExerciseId).distinct().toList()).stream()
                .collect(Collectors.toMap(Exercise::getId, exercise -> exercise, (first, second) -> first));

        Map<Long, List<Set>> setsByExercise = sets.stream()
                .collect(Collectors.groupingBy(Set::getExerciseId));

        List<CoachRecommendationRequest.WorkoutSummary.ExerciseSummary> exerciseSummaries = setsByExercise.entrySet()
                .stream()
                .filter(entry -> exerciseMap.containsKey(entry.getKey()))
                .map(entry -> buildExerciseSummary(exerciseMap.get(entry.getKey()), entry.getValue()))
                .toList();

        return new CoachRecommendationRequest.WorkoutSummary(
                workout.getStartTime().format(WORKOUT_DATE_FORMATTER),
                exerciseSummaries);
    }

    private CoachRecommendationRequest.WorkoutSummary.ExerciseSummary buildExerciseSummary(
            Exercise exercise,
            List<Set> sets) {
        double avgReps = sets.stream()
                .mapToInt(set -> set.getReps() != null ? set.getReps() : 0)
                .average()
                .orElse(0);

        double avgWeight = sets.stream()
                .mapToDouble(set -> set.getWeight() != null ? set.getWeight() : 0)
                .average()
                .orElse(0);

        double avgRPE = sets.stream()
                .mapToDouble(set -> set.getRpe() != null ? set.getRpe() : 0)
                .average()
                .orElse(0);

        return new CoachRecommendationRequest.WorkoutSummary.ExerciseSummary(
                exercise.getName(),
                exercise.getCategory(),
                sets.size(),
                avgReps,
                avgWeight,
                avgRPE);
    }

    // 503s are treated as not worth retrying by the Kafka consumer; anything else is retried.
    private ResponseStatusException mapModelError(HttpClientErrorException e) {
        int status = e.getStatusCode().value();
        if (status == 429) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI coach is busy (Gemini free-tier rate limit or daily quota reached). Try again later.");
        }
        if (status == 401 || status == 403
                || (status == 400 && e.getResponseBodyAsString().contains("API key"))) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI coach is not configured (invalid Gemini API key)");
        }
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "AI coach request failed: " + e.getStatusText());
    }

    private String buildPrompt(CoachRecommendationRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert fitness coach. Based on the user's recent workout history, ");
        prompt.append("provide personalized recommendations.\n\n");

        prompt.append("User Profile:\n");
        prompt.append("- Fitness Level: ").append(request.fitnessLevel()).append("\n");
        prompt.append("- Goals: ").append(request.userGoals()).append("\n\n");

        prompt.append("Recent Workouts:\n");
        for (CoachRecommendationRequest.WorkoutSummary workout : request.recentWorkouts()) {
            prompt.append("- Date: ").append(workout.date()).append("\n");
            for (CoachRecommendationRequest.WorkoutSummary.ExerciseSummary exercise : workout.exercises()) {
                prompt.append("  * ").append(exercise.name())
                        .append(" (").append(exercise.category()).append(")")
                        .append(": ").append(exercise.totalSets()).append(" sets, ")
                        .append("avg ").append(exercise.averageReps()).append(" reps, ")
                        .append(exercise.averageWeight()).append(" lbs, ")
                        .append("RPE: ").append(exercise.averageRPE()).append("\n");
            }
        }

        prompt.append("\nProvide recommendations in this exact format, as plain text without markdown:\n");
        prompt.append("RECOMMENDATION: [2-3 sentence recommendation]\n");
        prompt.append("FOCUS_AREA: [primary area to focus on]\n");
        prompt.append("NEXT_EXERCISE: [one exercise suggestion for next workout]");

        return prompt.toString();
    }

    private CoachRecommendationResponse parseResponse(String modelReply) {
        // Models sometimes bold the labels ("**FOCUS_AREA:**") despite the prompt.
        String reply = modelReply.replace("**", "");
        String recommendation = extractSection(reply, "RECOMMENDATION:");
        String focusArea = extractSection(reply, "FOCUS_AREA:");
        String nextExercise = extractSection(reply, "NEXT_EXERCISE:");

        return new CoachRecommendationResponse(
                recommendation.trim(),
                focusArea.trim(),
                nextExercise.trim());
    }

    private String extractSection(String response, String marker) {
        int startIndex = response.indexOf(marker);
        if (startIndex == -1) {
            return "Unable to extract recommendation";
        }

        startIndex += marker.length();
        int endIndex = response.indexOf("\n", startIndex);

        if (endIndex == -1) {
            endIndex = response.length();
        }

        return response.substring(startIndex, endIndex);
    }
}
