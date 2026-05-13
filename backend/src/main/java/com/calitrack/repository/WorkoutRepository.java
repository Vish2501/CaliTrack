package com.calitrack.repository;

import com.calitrack.entity.Workout;
import com.calitrack.projection.WorkoutFrequencyView;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

        List<Workout> findByUserId(String userId);

        Optional<Workout> findByIdAndUserId(Long id, String userId);

        @Query(value = """
                        SELECT CAST(date_trunc('week', w.start_time) AS date) AS weekStart,
                                   COUNT(*) AS workoutCount
                            FROM workouts w
                            WHERE w.user_id = :userId
                              AND w.start_time BETWEEN :start AND :end
                            GROUP BY weekStart
                            ORDER BY weekStart
                        """, nativeQuery = true)
        List<WorkoutFrequencyView> findWorkoutFrequency(
                        @Param("userId") String userId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);
}
