package com.calitrack.service;

import com.calitrack.dto.CreateExerciseRequest;
import com.calitrack.entity.Exercise;
import com.calitrack.repository.ExerciseRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    // Names are unique for each user, ignoring case. Reusing an archived exercise's name restores
    // that exercise, so its history carries on.
    @Transactional
    public Exercise createExercise(CreateExerciseRequest request, String userId) {
        String name = request.getName().trim();

        Optional<Exercise> sameName = exerciseRepository.findByUserIdAndNameIgnoreCase(userId, name);
        if (sameName.isPresent()) {
            Exercise existing = sameName.get();
            if (!existing.isArchived()) {
                throw nameTaken();
            }
            existing.setArchived(false);
            return exerciseRepository.save(existing);
        }

        Exercise exercise = new Exercise();
        exercise.setName(name);
        exercise.setCategory(request.getCategory() != null ? request.getCategory().trim() : null);
        exercise.setUserId(userId);
        try {
            // Flushed here so the unique index rejects a simultaneous create with the same name now.
            return exerciseRepository.saveAndFlush(exercise);
        } catch (DataIntegrityViolationException e) {
            throw nameTaken();
        }
    }

    private static ResponseStatusException nameTaken() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "An exercise with this name already exists");
    }

    public List<Exercise> getExercisesForUser(String userId) {
        return exerciseRepository.findByUserIdAndArchivedFalse(userId);
    }

    // Archives the exercise. Its sets stay, so past workouts still show it.
    @Transactional
    public void deleteExercise(Long id, String userId) {
        Exercise exercise = exerciseRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));

        exercise.setArchived(true);
        exerciseRepository.save(exercise);
    }
}
