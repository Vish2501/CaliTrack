package com.calitrack.service;

import com.calitrack.dto.CreateSetRequest;
import com.calitrack.dto.SetResponse;
import com.calitrack.dto.UpdateSetRequest;
import com.calitrack.entity.Exercise;
import com.calitrack.entity.Set;
import com.calitrack.entity.Workout;
import com.calitrack.repository.ExerciseRepository;
import com.calitrack.repository.SetRepository;
import com.calitrack.repository.WorkoutRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SetService {

    private final SetRepository setRepository;
    private final WorkoutRepository workoutRepository;
    private final ExerciseRepository exerciseRepository;

    public SetService(SetRepository setRepository,
                      WorkoutRepository workoutRepository,
                      ExerciseRepository exerciseRepository) {
        this.setRepository = setRepository;
        this.workoutRepository = workoutRepository;
        this.exerciseRepository = exerciseRepository;
    }

    public SetResponse addSet(Long workoutId, String userId, CreateSetRequest request) {
        Workout workout = workoutRepository.findByIdAndUserId(workoutId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));

        Exercise exercise = exerciseRepository.findByIdAndUserId(request.exerciseId(), userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));

        Set set = new Set();
        set.setWorkoutId(workout.getId());
        set.setExerciseId(exercise.getId());
        set.setReps(request.reps());
        set.setWeight(request.weight());
        set.setRpe(request.rpe());
        set.setTimestamp(request.timestamp() != null ? request.timestamp() : LocalDateTime.now());

        Set saved = setRepository.save(set);
        return toResponse(saved, exercise.getName());
    }

    public List<SetResponse> getSets(Long workoutId, String userId) {
        workoutRepository.findByIdAndUserId(workoutId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));

        List<Set> sets = setRepository.findByWorkoutId(workoutId);
        Map<Long, String> exerciseNames = exerciseRepository.findAllById(
            sets.stream().map(Set::getExerciseId).distinct().toList()
        ).stream().collect(Collectors.toMap(Exercise::getId, Exercise::getName, (a, b) -> a));

        return sets.stream()
            .map(s -> toResponse(s, exerciseNames.getOrDefault(s.getExerciseId(), "Unknown")))
            .toList();
    }

    public SetResponse updateSet(Long workoutId, Long setId, String userId, UpdateSetRequest request) {
        workoutRepository.findByIdAndUserId(workoutId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));

        Set set = setRepository.findByIdAndWorkoutId(setId, workoutId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Set not found"));

        if (request.reps() != null) set.setReps(request.reps());
        if (request.weight() != null) set.setWeight(request.weight());
        if (request.rpe() != null) set.setRpe(request.rpe());
        if (request.timestamp() != null) set.setTimestamp(request.timestamp());

        Set saved = setRepository.save(set);
        String exerciseName = exerciseRepository.findById(saved.getExerciseId())
            .map(Exercise::getName)
            .orElse("Unknown");
        return toResponse(saved, exerciseName);
    }

    public void deleteSet(Long workoutId, Long setId, String userId) {
        workoutRepository.findByIdAndUserId(workoutId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));

        Set set = setRepository.findByIdAndWorkoutId(setId, workoutId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Set not found"));

        setRepository.delete(set);
    }

    private SetResponse toResponse(Set set, String exerciseName) {
        return new SetResponse(
            set.getId(),
            set.getWorkoutId(),
            set.getExerciseId(),
            exerciseName,
            set.getReps(),
            set.getWeight(),
            set.getRpe(),
            set.getTimestamp()
        );
    }
}
