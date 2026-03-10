package com.calitrack.service;

import com.calitrack.entity.Exercise;
import com.calitrack.repository.ExerciseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    public Exercise createExercise(Exercise exercise) {
        return exerciseRepository.save(exercise);
    }

    public List<Exercise> getExercisesForUser(String userId) {
        return exerciseRepository.findByUserId(userId);
    }

    public void deleteExercise(Long id, String userId) {
        Exercise ex = exerciseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exercise not found"));

        if (!ex.getUserId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }

        exerciseRepository.deleteById(id);
    }
}
