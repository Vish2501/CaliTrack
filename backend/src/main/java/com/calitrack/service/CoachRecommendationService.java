package com.calitrack.service;

import com.calitrack.dto.CoachProfileResponse;
import com.calitrack.dto.CoachRecommendationResponse;
import com.calitrack.dto.LatestCoachRecommendationResponse;
import com.calitrack.dto.SavedCoachRecommendationResponse;
import com.calitrack.entity.CoachRecommendation;
import com.calitrack.event.WorkoutLoggedEvent;
import com.calitrack.exception.NonRetryableCoachException;
import com.calitrack.repository.CoachRecommendationRepository;
import com.calitrack.repository.WorkoutRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@Service
public class CoachRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(CoachRecommendationService.class);

    private static final int WORKOUTS_TO_ANALYSE = 5;
    // Long enough to cover retries with backoff; after that an event has most likely gone to the
    // DLT, and the app should stop showing "generating".
    static final Duration PENDING_WINDOW = Duration.ofMinutes(2);

    private final AICoachService aiCoachService;
    private final CoachProfileService coachProfileService;
    private final CoachRecommendationRepository recommendationRepository;
    private final WorkoutRepository workoutRepository;
    private final boolean asyncEnabled;

    public CoachRecommendationService(AICoachService aiCoachService,
                                      CoachProfileService coachProfileService,
                                      CoachRecommendationRepository recommendationRepository,
                                      WorkoutRepository workoutRepository,
                                      @Value("${calitrack.coach.async-enabled}") boolean asyncEnabled) {
        this.aiCoachService = aiCoachService;
        this.coachProfileService = coachProfileService;
        this.recommendationRepository = recommendationRepository;
        this.workoutRepository = workoutRepository;
        this.asyncEnabled = asyncEnabled;
    }

    public LatestCoachRecommendationResponse getLatest(String userId) {
        SavedCoachRecommendationResponse latest = recommendationRepository.findFirstByUserIdOrderByIdDesc(userId)
            .map(r -> new SavedCoachRecommendationResponse(
                r.getWorkoutId(), r.getRecommendation(), r.getFocusArea(), r.getNextExercise(), r.getCreatedAt()))
            .orElse(null);
        return new LatestCoachRecommendationResponse(latest, isPending(userId));
    }

    // True when the user's most recently finished workout has no recommendation yet and was
    // finished recently enough that the consumer may still be working on it.
    private boolean isPending(String userId) {
        if (!asyncEnabled) {
            return false;
        }
        return workoutRepository.findFirstByUserIdAndEndTimeIsNotNullOrderByEndTimeDesc(userId)
            .filter(workout -> workout.getEndTime().isAfter(Instant.now().minus(PENDING_WINDOW)))
            .map(workout -> !recommendationRepository.existsByWorkoutId(workout.getId()))
            .orElse(false);
    }

    /**
     * Generates and stores the recommendation for a finished workout. Safe to call more than once
     * for the same workout: a redelivered event is skipped before calling the model, and the unique
     * constraint on workout_id catches two deliveries racing each other.
     */
    public void generateFor(WorkoutLoggedEvent event) {
        if (recommendationRepository.existsByWorkoutId(event.workoutId())) {
            log.info("Recommendation for workout {} already exists, skipping", event.workoutId());
            return;
        }

        CoachProfileResponse profile = coachProfileService.getProfile(event.userId());

        CoachRecommendationResponse response;
        try {
            response = aiCoachService.getAutoRecommendation(
                event.userId(), WORKOUTS_TO_ANALYSE, profile.goals(), profile.fitnessLevel());
        } catch (ResponseStatusException e) {
            // AICoachService reports configuration and quota problems as 503; anything else
            // (network errors, timeouts, Gemini 5xx) is worth retrying.
            if (e.getStatusCode().isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE)) {
                throw new NonRetryableCoachException(e.getReason(), e);
            }
            throw e;
        }

        CoachRecommendation recommendation = new CoachRecommendation();
        recommendation.setUserId(event.userId());
        recommendation.setWorkoutId(event.workoutId());
        recommendation.setRecommendation(response.recommendation());
        recommendation.setFocusArea(response.focusArea());
        recommendation.setNextExercise(response.suggestedNextExercise());

        try {
            recommendationRepository.saveAndFlush(recommendation);
        } catch (DataIntegrityViolationException e) {
            // Unique or foreign key violation: another delivery saved it first, or the workout
            // was deleted. Either way there is nothing to retry.
            log.info("Recommendation for workout {} not saved: already exists or workout deleted",
                event.workoutId());
        }
    }
}
