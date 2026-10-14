package com.calitrack.kafka;

import com.calitrack.event.WorkoutLoggedEvent;
import com.calitrack.service.CoachRecommendationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.calitrack.config.KafkaTopicConfig.WORKOUT_LOGGED_TOPIC;

@Component
@ConditionalOnProperty(name = "calitrack.coach.async-enabled", havingValue = "true")
public class WorkoutLoggedConsumer {

    private final CoachRecommendationService coachRecommendationService;

    public WorkoutLoggedConsumer(CoachRecommendationService coachRecommendationService) {
        this.coachRecommendationService = coachRecommendationService;
    }

    // Exceptions go to the DefaultErrorHandler in KafkaConsumerConfig: retried with backoff,
    // then published to workout-logged.DLT.
    @KafkaListener(topics = WORKOUT_LOGGED_TOPIC, groupId = "coach-recommender")
    public void onWorkoutLogged(WorkoutLoggedEvent event) {
        coachRecommendationService.generateFor(event);
    }
}
