package com.calitrack.service;

import com.calitrack.dto.PreviousSetResponse;
import com.calitrack.dto.SetResponse;
import com.calitrack.dto.UpdateSetRequest;
import com.calitrack.entity.Exercise;
import com.calitrack.entity.Set;
import com.calitrack.repository.ExerciseRepository;
import com.calitrack.repository.SetRepository;
import com.calitrack.repository.WorkoutRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    // The sets from the last time this user did this exercise, in order, so the phone can suggest
    // values for today's sets. Empty when they have never done it.
    public List<PreviousSetResponse> getPreviousSets(Long exerciseId, String userId) {
        exerciseRepository.findByIdAndUserId(exerciseId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));

        Optional<Long> latestWorkoutId = setRepository.findLatestWorkoutIdWithExercise(userId, exerciseId);
        if (latestWorkoutId.isEmpty()) {
            return List.of(); // never done this exercise
        }

        return setRepository.findByWorkoutIdAndExerciseIdOrderByPosition(latestWorkoutId.get(), exerciseId)
            .stream()
            .map(set -> new PreviousSetResponse(set.getReps(), set.getWeight()))
            .toList();
    }

    public List<SetResponse> getSets(Long workoutId, String userId) {
        workoutRepository.findByIdAndUserId(workoutId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));

        List<Set> sets = setRepository.findByWorkoutIdOrderByPosition(workoutId);
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
            set.getRpe()
        );
    }
}
