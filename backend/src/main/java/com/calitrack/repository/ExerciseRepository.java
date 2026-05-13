package com.calitrack.repository;

import com.calitrack.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findByUserId(String userId);
Optional<Exercise> findByIdAndUserId(Long id, String userId);
    
}
