package com.calitrack.service;

import com.calitrack.dto.CoachRecommendationResponse;
import com.calitrack.entity.CoachProfile;
import com.calitrack.entity.CoachRecommendation;
import com.calitrack.event.WorkoutLoggedEvent;
import com.calitrack.exception.NonRetryableCoachException;
import com.calitrack.repository.CoachProfileRepository;
import com.calitrack.repository.CoachRecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CoachRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(CoachRecommendationService.class);

    private static final int WORKOUTS_TO_ANALYSE = 5;
    private static final String DEFAULT_GOALS = "general fitness";
    private static final String DEFAULT_FITNESS_LEVEL = "intermediate";

    private final AICoachService aiCoachService;
    private final CoachRecommendationRepository recommendationRepository;
    private final CoachProfileRepository profileRepository;

    public CoachRecommendationService(AICoachService aiCoachService,
                                      CoachRecommendationRepository recommendationRepository,
                                      CoachProfileRepository profileRepository) {
        this.aiCoachService = aiCoachService;
        this.recommendationRepository = recommendationRepository;
        this.profileRepository = profileRepository;
    }

    /**
     * Generates and stores the recommendation for a finished workout. Safe to call more than once
     * for the same workout: a redelivered event is skipped before calling OpenAI, and the unique
     * constraint on workout_id catches two deliveries racing each other.
     */
    public void generateFor(WorkoutLoggedEvent event) {
        if (recommendationRepository.existsByWorkoutId(event.workoutId())) {
            log.info("Recommendation for workout {} already exists, skipping", event.workoutId());
            return;
        }

        CoachProfile profile = profileRepository.findById(event.userId()).orElse(null);
        String goals = profile != null ? profile.getGoals() : DEFAULT_GOALS;
        String fitnessLevel = profile != null ? profile.getFitnessLevel() : DEFAULT_FITNESS_LEVEL;

        CoachRecommendationResponse response;
        try {
            response = aiCoachService.getAutoRecommendation(
                event.userId(), WORKOUTS_TO_ANALYSE, goals, fitnessLevel);
        } catch (ResponseStatusException e) {
            // AICoachService reports configuration and quota problems as 503; anything else
            // (network errors, OpenAI 5xx, rate limits) is worth retrying.
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
