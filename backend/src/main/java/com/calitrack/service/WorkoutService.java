package com.calitrack.service;

import com.calitrack.dto.SetResponse;
import com.calitrack.dto.WorkoutDetailsResponse;
import com.calitrack.entity.Exercise;
import com.calitrack.entity.Set;
import com.calitrack.entity.Workout;
import com.calitrack.event.WorkoutLoggedEvent;
import com.calitrack.repository.ExerciseRepository;
import com.calitrack.repository.SetRepository;
import com.calitrack.repository.WorkoutRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final SetRepository setRepository;
    private final ExerciseRepository exerciseRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WorkoutService(WorkoutRepository workoutRepository,
                          SetRepository setRepository,
                          ExerciseRepository exerciseRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.workoutRepository = workoutRepository;
        this.setRepository = setRepository;
        this.exerciseRepository = exerciseRepository;
        this.eventPublisher = eventPublisher;
    }

    public Workout startWorkout(String userId) {
        Workout workout = new Workout();
        workout.setUserId(userId);
        workout.setStartTime(Instant.now());

        return workoutRepository.save(workout);
    }

    public List<Workout> getWorkoutsForUser(String userId) {
        return workoutRepository.findByUserId(userId);
    }

    public WorkoutDetailsResponse getWorkoutDetails(Long workoutId, String userId) {
        Workout workout = workoutRepository.findByIdAndUserId(workoutId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));

        List<Set> sets = setRepository.findByWorkoutId(workoutId);
        Map<Long, String> exerciseNames = exerciseRepository.findAllById(
            sets.stream().map(Set::getExerciseId).distinct().toList()
        ).stream().collect(Collectors.toMap(Exercise::getId, Exercise::getName, (a, b) -> a));

        List<SetResponse> setResponses = sets.stream()
            .map(s -> new SetResponse(
                s.getId(),
                s.getWorkoutId(),
                s.getExerciseId(),
                exerciseNames.getOrDefault(s.getExerciseId(), "Unknown"),
                s.getReps(),
                s.getWeight(),
                s.getRpe(),
                s.getTimestamp()
            ))
            .toList();

        return new WorkoutDetailsResponse(
            workout.getId(),
            workout.getUserId(),
            workout.getStartTime(),
            workout.getEndTime(),
            workout.getNotes(),
            setResponses
        );
    }

    // Transactional so the WorkoutLogged listener can wait for the commit (AFTER_COMMIT
    // listeners never fire outside a transaction).
    @Transactional
    public Workout finishWorkout(Long workoutId, String userId, Instant endedAt) {
        Workout workout = workoutRepository.findByIdAndUserId(workoutId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));

        workout.setEndTime(endedAt != null ? endedAt : Instant.now());
        Workout saved = workoutRepository.save(workout);
        eventPublisher.publishEvent(new WorkoutLoggedEvent(saved.getId(), userId, saved.getEndTime()));
        return saved;
    }

    public Workout updateWorkout(Long workoutId, String userId, String notes) {
        Workout workout = workoutRepository.findByIdAndUserId(workoutId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));

        workout.setNotes(notes);
        return workoutRepository.save(workout);
    }
}
