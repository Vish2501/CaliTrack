package com.calitrack.repository;

import com.calitrack.entity.Set;
import com.calitrack.projection.HeaviestPrView;
import com.calitrack.projection.SetVolumePrView;
import com.calitrack.projection.WeeklyVolumeView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SetRepository extends JpaRepository<Set, Long> {

    // ---- Sets of a workout (queries built from the method names) ----

    List<Set> findByWorkoutIdOrderByPosition(Long workoutId);

    Optional<Set> findByIdAndWorkoutId(Long id, Long workoutId);

    // ---- Previous sets of an exercise ----

    // The ID of this user's most recent workout that has a set of this exercise.
    @Query(value = """
        SELECT w.id
        FROM workouts w
        JOIN sets s ON s.workout_id = w.id
        WHERE w.user_id = :userId
          AND s.exercise_id = :exerciseId
        ORDER BY w.end_time DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<Long> findLatestWorkoutIdWithExercise(
        @Param("userId") String userId,
        @Param("exerciseId") Long exerciseId);

    // That workout's sets of this exercise, in the order they were done.
    List<Set> findByWorkoutIdAndExerciseIdOrderByPosition(Long workoutId, Long exerciseId);

    // ---- Analytics ----

    @Query(value = """
        SELECT s.exercise_id AS exerciseId,
               e.name AS exerciseName,
               SUM(s.reps * s.weight) AS totalVolume
        FROM sets s
        JOIN workouts w ON s.workout_id = w.id
        JOIN exercises e ON s.exercise_id = e.id
        WHERE w.user_id = :userId
          AND w.start_time >= :start AND w.start_time < :end
        GROUP BY s.exercise_id, e.name
        """, nativeQuery = true)
    List<WeeklyVolumeView> findWeeklyVolume(
        @Param("userId") String userId,
        @Param("start") Instant start,
        @Param("end") Instant end);

    @Query(value = """
        SELECT s.exercise_id AS exerciseId,
               e.name AS exerciseName,
               MAX(s.weight) AS maxWeight
        FROM sets s
        JOIN workouts w ON s.workout_id = w.id
        JOIN exercises e ON s.exercise_id = e.id
        WHERE w.user_id = :userId
          AND s.weight IS NOT NULL
        GROUP BY s.exercise_id, e.name
        """, nativeQuery = true)
    List<HeaviestPrView> findHeaviestPrs(@Param("userId") String userId);

    @Query(value = """
        SELECT s.exercise_id AS exerciseId,
               e.name AS exerciseName,
               MAX(s.reps * s.weight) AS maxSetVolume
        FROM sets s
        JOIN workouts w ON s.workout_id = w.id
        JOIN exercises e ON s.exercise_id = e.id
        WHERE w.user_id = :userId
          AND s.weight IS NOT NULL
        GROUP BY s.exercise_id, e.name
        """, nativeQuery = true)
    List<SetVolumePrView> findMaxSetVolume(@Param("userId") String userId);
}
