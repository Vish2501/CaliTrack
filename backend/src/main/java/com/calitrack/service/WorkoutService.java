package com.calitrack.service;

import com.calitrack.dto.FinishWorkoutRequest;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

    public List<Workout> getWorkoutsForUser(String userId) {
        return workoutRepository.findByUserId(userId);
    }

    public WorkoutDetailsResponse getWorkoutDetails(Long workoutId, String userId) {
        Workout workout = workoutRepository.findByIdAndUserId(workoutId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));
        return toDetails(workout);
    }

    // The workout this user already finished with this phone-generated ID, if any.
    public Optional<WorkoutDetailsResponse> findFinishedWorkout(String userId, UUID clientId) {
        return workoutRepository.findByUserIdAndClientId(userId, clientId).map(this::toDetails);
    }

    // Saves the workout and all its sets in one transaction, so a failure saves nothing.
    // Transactional also lets the WorkoutLogged listener wait for the commit (AFTER_COMMIT
    // listeners never fire outside a transaction).
    @Transactional
    public WorkoutDetailsResponse finishWorkout(String userId, FinishWorkoutRequest request) {
        if (request.endedAt().isBefore(request.startedAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endedAt must not be before startedAt");
        }
        requireOwnExercises(userId, request.sets());

        Workout workout = new Workout();
        workout.setUserId(userId);
        workout.setClientId(request.id());
        workout.setStartTime(request.startedAt());
        workout.setEndTime(request.endedAt());
        workout.setNotes(request.notes());
        Workout saved = workoutRepository.save(workout);

        List<FinishWorkoutRequest.FinishedSet> finishedSets = request.sets();
        for (int position = 0; position < finishedSets.size(); position++) {
            FinishWorkoutRequest.FinishedSet finishedSet = finishedSets.get(position);
            Set set = new Set();
            set.setWorkoutId(saved.getId());
            set.setExerciseId(finishedSet.exerciseId());
            set.setReps(finishedSet.reps());
            set.setWeight(finishedSet.weight());
            set.setPosition(position);
            setRepository.save(set);
        }

        eventPublisher.publishEvent(new WorkoutLoggedEvent(saved.getId(), userId, saved.getEndTime()));
        return toDetails(saved);
    }

    // Another user's exercise is reported like a missing one, so its ID isn't revealed.
    private void requireOwnExercises(String userId, List<FinishWorkoutRequest.FinishedSet> sets) {
        List<Long> exerciseIds = sets.stream().map(FinishWorkoutRequest.FinishedSet::exerciseId).distinct().toList();
        long owned = exerciseRepository.findAllById(exerciseIds).stream()
            .filter(exercise -> exercise.getUserId().equals(userId))
            .count();
        if (owned != exerciseIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown exercise");
        }
    }

    public Workout updateWorkout(Long workoutId, String userId, String notes) {
        Workout workout = workoutRepository.findByIdAndUserId(workoutId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));

        workout.setNotes(notes);
        return workoutRepository.save(workout);
    }

    private WorkoutDetailsResponse toDetails(Workout workout) {
        List<Set> sets = setRepository.findByWorkoutIdOrderByPosition(workout.getId());
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
                s.getRpe()
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
}
