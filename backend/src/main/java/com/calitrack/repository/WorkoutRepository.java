package com.calitrack.repository;

import com.calitrack.entity.Workout;
import com.calitrack.projection.WorkoutFrequencyView;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

        List<Workout> findByUserId(String userId);

        Optional<Workout> findByIdAndUserId(Long id, String userId);

        Optional<Workout> findFirstByUserIdAndEndTimeIsNotNullOrderByEndTimeDesc(String userId);

        // Converting to the user's local time before truncating makes weeks run from Monday 00:00
        // in their zone (Postgres weeks start on Monday).
        @Query(value = """
                        SELECT CAST(date_trunc('week', w.start_time AT TIME ZONE :timeZone) AS date) AS weekStart,
                                   COUNT(*) AS workoutCount
                            FROM workouts w
                            WHERE w.user_id = :userId
                              AND w.start_time >= :start AND w.start_time < :end
                            GROUP BY weekStart
                            ORDER BY weekStart
                        """, nativeQuery = true)
        List<WorkoutFrequencyView> findWorkoutFrequency(
                        @Param("userId") String userId,
                        @Param("start") Instant start,
                        @Param("end") Instant end,
                        @Param("timeZone") String timeZone);
}
