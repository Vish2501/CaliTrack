
package com.calitrack.repository;

import com.calitrack.entity.Set;
import com.calitrack.projection.WeeklyVolumeView;
import com.calitrack.projection.HeaviestPrView;
import com.calitrack.projection.SetVolumePrView;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.time.LocalDateTime;
import java.util.List;

public interface SetRepository extends JpaRepository<Set, Long> {

  List<Set> findByWorkoutId(Long workoutId);
Optional<Set> findByIdAndWorkoutId(Long id, Long workoutId);
  @Query(value = """
          SELECT s.exercise_id AS exerciseId,
             e.name AS exerciseName,
             SUM(s.reps * s.weight) AS totalVolume
      FROM sets s
      JOIN workouts w ON s.workout_id = w.id
      JOIN exercises e ON s.exercise_id = e.id
      WHERE w.user_id = :userId
        AND w.start_time BETWEEN :start AND :end
      GROUP BY s.exercise_id, e.name

      """, nativeQuery = true)
  List<WeeklyVolumeView> findWeeklyVolume(
      @Param("userId") String userId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

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
  List<SetVolumePrView> findMaxSetVolume(
      @Param("userId") String userId);

}
