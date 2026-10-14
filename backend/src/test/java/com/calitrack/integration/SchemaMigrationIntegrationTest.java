package com.calitrack.integration;

import com.calitrack.entity.CoachProfile;
import com.calitrack.entity.CoachRecommendation;
import com.calitrack.entity.Workout;
import com.calitrack.repository.CoachProfileRepository;
import com.calitrack.repository.CoachRecommendationRepository;
import com.calitrack.repository.WorkoutRepository;
import com.calitrack.support.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs the real Flyway migrations instead of the test profile's create-drop, and
// ddl-auto=validate fails context startup if the entities don't match them.
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class SchemaMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private CoachRecommendationRepository recommendationRepository;

    @Autowired
    private CoachProfileRepository profileRepository;

    @Test
    void migrationsApplyUpToLatestVersion() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
    }

    @Test
    void secondRecommendationForSameWorkoutIsRejected() {
        Workout workout = saveWorkout("unique-check-user");
        recommendationRepository.saveAndFlush(recommendationFor(workout));

        assertThatThrownBy(() -> recommendationRepository.saveAndFlush(recommendationFor(workout)))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(recommendationRepository.existsByWorkoutId(workout.getId())).isTrue();
    }

    @Test
    void latestRecommendationIsTheMostRecentForThatUser() {
        Workout first = saveWorkout("latest-user");
        Workout second = saveWorkout("latest-user");
        Workout otherUsers = saveWorkout("someone-else");
        recommendationRepository.saveAndFlush(recommendationFor(first));
        recommendationRepository.saveAndFlush(recommendationFor(second));
        recommendationRepository.saveAndFlush(recommendationFor(otherUsers));

        assertThat(recommendationRepository.findFirstByUserIdOrderByIdDesc("latest-user"))
            .get()
            .extracting(CoachRecommendation::getWorkoutId)
            .isEqualTo(second.getId());
    }

    @Test
    void profileRoundTrips() {
        CoachProfile profile = new CoachProfile();
        profile.setUserId("profile-user");
        profile.setGoals("strength");
        profile.setFitnessLevel("beginner");
        profileRepository.saveAndFlush(profile);

        assertThat(profileRepository.findById("profile-user"))
            .get()
            .satisfies(saved -> {
                assertThat(saved.getGoals()).isEqualTo("strength");
                assertThat(saved.getFitnessLevel()).isEqualTo("beginner");
                assertThat(saved.getUpdatedAt()).isNotNull();
            });
    }

    private Workout saveWorkout(String userId) {
        Workout workout = new Workout();
        workout.setUserId(userId);
        workout.setStartTime(LocalDateTime.now().minusHours(1));
        workout.setEndTime(LocalDateTime.now());
        return workoutRepository.saveAndFlush(workout);
    }

    private CoachRecommendation recommendationFor(Workout workout) {
        CoachRecommendation recommendation = new CoachRecommendation();
        recommendation.setUserId(workout.getUserId());
        recommendation.setWorkoutId(workout.getId());
        recommendation.setRecommendation("Add a set of pull-ups.");
        recommendation.setFocusArea("Back");
        recommendation.setNextExercise("Chin-ups");
        return recommendation;
    }
}
