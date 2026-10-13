package com.calitrack.service;

import com.calitrack.dto.CreateExerciseRequest;
import com.calitrack.entity.Exercise;
import com.calitrack.repository.ExerciseRepository;
import com.calitrack.repository.SetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final SetRepository setRepository;

    public ExerciseService(ExerciseRepository exerciseRepository, SetRepository setRepository) {
        this.exerciseRepository = exerciseRepository;
        this.setRepository = setRepository;
    }

    public Exercise createExercise(Exercise exercise) {
        return exerciseRepository.save(exercise);
    }

    public Exercise createExercise(CreateExerciseRequest request, String userId) {
        Exercise exercise = new Exercise();
        exercise.setName(request.getName().trim());
        exercise.setCategory(request.getCategory() != null ? request.getCategory().trim() : null);
        exercise.setUserId(userId);

        return exerciseRepository.save(exercise);
    }

    public List<Exercise> getExercisesForUser(String userId) {
        return exerciseRepository.findByUserId(userId);
    }

    @Transactional
    public void deleteExercise(Long id, String userId) {
        Exercise ex = exerciseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));

        if (!ex.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        setRepository.deleteByExerciseId(id);
        exerciseRepository.deleteById(id);
    }
}
