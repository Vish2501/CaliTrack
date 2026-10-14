package com.calitrack.kafka;

import com.calitrack.event.WorkoutLoggedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.calitrack.config.KafkaTopicConfig.WORKOUT_LOGGED_TOPIC;

@Component
@ConditionalOnProperty(name = "calitrack.coach.async-enabled", havingValue = "true")
public class WorkoutLoggedPublisher {

    private static final Logger log = LoggerFactory.getLogger(WorkoutLoggedPublisher.class);

    private final KafkaTemplate<String, WorkoutLoggedEvent> kafkaTemplate;

    public WorkoutLoggedPublisher(KafkaTemplate<String, WorkoutLoggedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Runs only after the finishWorkout transaction commits, so nothing is published for a
     * workout that rolled back. Keyed by user ID so each user's events stay in order on one
     * partition. Failures are logged rather than thrown: the workout is already saved, and an
     * exception here would turn a successful request into a 500.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(WorkoutLoggedEvent event) {
        try {
            kafkaTemplate.send(WORKOUT_LOGGED_TOPIC, event.userId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish WorkoutLogged for workout {}", event.workoutId(), ex);
                    }
                });
        } catch (RuntimeException ex) {
            log.error("Failed to publish WorkoutLogged for workout {}", event.workoutId(), ex);
        }
    }
}
