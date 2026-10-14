package com.calitrack.repository;

import com.calitrack.entity.CoachRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoachRecommendationRepository extends JpaRepository<CoachRecommendation, Long> {

    boolean existsByWorkoutId(Long workoutId);

    Optional<CoachRecommendation> findFirstByUserIdOrderByIdDesc(String userId);
}
