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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

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
    private DataSource dataSource;

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private CoachRecommendationRepository recommendationRepository;

    @Autowired
    private CoachProfileRepository profileRepository;

    @Test
    void migrationsApplyUpToLatestVersion() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("4");
    }

    @Test
    void v4GivesExistingWorkoutsAnIdAnEndTimeAndOrderedSets() {
        // A schema of its own, so the migrations can be stopped at V3 and data written the old way.
        String schema = "before_v4";
        Flyway.configure().dataSource(dataSource).schemas(schema).target("3").load().migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String started = "2026-06-01 09:00:00+00";
        jdbc.update("INSERT INTO " + schema + ".workouts (id, user_id, start_time, end_time) VALUES "
            + "(1, 'u', ?::timestamptz, ?::timestamptz), (2, 'u', ?::timestamptz, NULL)",
            started, "2026-06-01 10:00:00+00", started);
        jdbc.update("INSERT INTO " + schema + ".exercises (id, name, user_id) VALUES (1, 'Squat', 'u')");
        jdbc.update("INSERT INTO " + schema + ".sets (id, workout_id, exercise_id, reps, timestamp) VALUES "
            + "(10, 1, 1, 5, now()), (11, 1, 1, 6, now()), (12, 2, 1, 7, now())");

        Flyway.configure().dataSource(dataSource).schemas(schema).target("4").load().migrate();

        // The unfinished workout ends when it started; the finished one keeps its end.
        assertThat(momentIn(schema, "workouts", "end_time", "id = 1")).isEqualTo(Instant.parse("2026-06-01T10:00:00Z"));
        assertThat(momentIn(schema, "workouts", "end_time", "id = 2")).isEqualTo(Instant.parse("2026-06-01T09:00:00Z"));
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(DISTINCT client_id) FROM " + schema + ".workouts WHERE client_id IS NOT NULL", Long.class))
            .isEqualTo(2L);
        // Each workout's sets are numbered from 0 in the order they were saved.
        assertThat(jdbc.queryForList(
            "SELECT position FROM " + schema + ".sets ORDER BY id", Integer.class))
            .containsExactly(0, 1, 0);
    }

    @Test
    void timesWrittenBeforeV3KeepTheirUtcMoments() {
        // A schema of its own, so the migrations can be stopped at V2 and data written the old way.
        String schema = "before_v3";
        Flyway.configure().dataSource(dataSource).schemas(schema).target("2").load().migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String utcNoon = "2026-06-01 12:00:00";
        jdbc.update("INSERT INTO " + schema + ".workouts (id, user_id, start_time, end_time) VALUES (1, 'u', ?::timestamp, ?::timestamp)", utcNoon, utcNoon);
        jdbc.update("INSERT INTO " + schema + ".exercises (id, name, user_id) VALUES (1, 'Squat', 'u')");
        jdbc.update("INSERT INTO " + schema + ".sets (workout_id, exercise_id, timestamp) VALUES (1, 1, ?::timestamp)", utcNoon);
        jdbc.update("INSERT INTO " + schema + ".coach_recommendations (user_id, workout_id, recommendation, focus_area, next_exercise, created_at) VALUES ('u', 1, 'r', 'f', 'n', ?::timestamp)", utcNoon);
        jdbc.update("INSERT INTO " + schema + ".coach_profiles (user_id, goals, fitness_level, updated_at) VALUES ('u', 'g', 'l', ?::timestamp)", utcNoon);

        // A session zone far from UTC proves V3 doesn't read old values in the session's zone.
        Flyway.configure().dataSource(dataSource).schemas(schema)
            .initSql("SET TIME ZONE 'Pacific/Auckland'")
            .target("3")
            .load().migrate();

        Instant utcNoonMoment = Instant.parse("2026-06-01T12:00:00Z");
        assertThat(momentIn(schema, "workouts", "start_time")).isEqualTo(utcNoonMoment);
        assertThat(momentIn(schema, "workouts", "end_time")).isEqualTo(utcNoonMoment);
        assertThat(momentIn(schema, "sets", "timestamp")).isEqualTo(utcNoonMoment);
        assertThat(momentIn(schema, "coach_recommendations", "created_at")).isEqualTo(utcNoonMoment);
        assertThat(momentIn(schema, "coach_profiles", "updated_at")).isEqualTo(utcNoonMoment);
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
        workout.setClientId(UUID.randomUUID());
        workout.setStartTime(Instant.now().minus(Duration.ofHours(1)));
        workout.setEndTime(Instant.now());
        return workoutRepository.saveAndFlush(workout);
    }

    private Instant momentIn(String schema, String table, String column) {
        return momentIn(schema, table, column, "true");
    }

    private Instant momentIn(String schema, String table, String column, String where) {
        return new JdbcTemplate(dataSource)
            .queryForObject("SELECT " + column + " FROM " + schema + "." + table + " WHERE " + where, OffsetDateTime.class)
            .toInstant();
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
