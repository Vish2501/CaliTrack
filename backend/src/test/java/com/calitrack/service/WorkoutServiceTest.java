package com.calitrack.service;

import com.calitrack.dto.FinishWorkoutRequest;
import com.calitrack.dto.WorkoutDetailsResponse;
import com.calitrack.entity.Exercise;
import com.calitrack.entity.Set;
import com.calitrack.entity.Workout;
import com.calitrack.event.WorkoutLoggedEvent;
import com.calitrack.repository.ExerciseRepository;
import com.calitrack.repository.SetRepository;
import com.calitrack.repository.WorkoutRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    @Mock
    private WorkoutRepository workoutRepository;

    @Mock
    private SetRepository setRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WorkoutService workoutService;

    @Test
    void getWorkoutDetailsReturnsSetsWithExerciseNames() {
        Workout workout = new Workout();
        workout.setId(10L);
        workout.setUserId("user-1");
        workout.setStartTime(Instant.now());

        Set set = new Set();
        set.setId(5L);
        set.setWorkoutId(10L);
        set.setExerciseId(20L);
        set.setReps(12);
        set.setWeight(0.0);
        set.setRpe(7.0);
        set.setPosition(0);

        Exercise exercise = new Exercise();
        exercise.setId(20L);
        exercise.setName("Pull-Up");

        when(workoutRepository.findByIdAndUserId(10L, "user-1")).thenReturn(Optional.of(workout));
        when(setRepository.findByWorkoutIdOrderByPosition(10L)).thenReturn(List.of(set));
        when(exerciseRepository.findAllById(List.of(20L))).thenReturn(List.of(exercise));

        WorkoutDetailsResponse details = workoutService.getWorkoutDetails(10L, "user-1");

        assertThat(details.id()).isEqualTo(10L);
        assertThat(details.sets()).hasSize(1);
        assertThat(details.sets().get(0).exerciseName()).isEqualTo("Pull-Up");
    }

    @Test
    void getWorkoutDetailsThrowsNotFoundWhenMissing() {
        when(workoutRepository.findByIdAndUserId(99L, "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutService.getWorkoutDetails(99L, "user-1"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Workout not found");
    }

    @Test
    void finishWorkoutSavesSetsInOrderAndPublishesWorkoutLogged() {
        Instant endedAt = Instant.parse("2026-06-01T10:00:00Z");
        when(exerciseRepository.findAllById(List.of(21L, 20L)))
            .thenReturn(List.of(exercise(20L, "user-1"), exercise(21L, "user-1")));
        when(workoutRepository.save(any(Workout.class))).thenAnswer(invocation -> {
            Workout workout = invocation.getArgument(0);
            workout.setId(10L);
            return workout;
        });

        workoutService.finishWorkout("user-1", request(endedAt.minus(Duration.ofHours(1)), endedAt,
            new FinishWorkoutRequest.FinishedSet(21L, 5, 40.0),
            new FinishWorkoutRequest.FinishedSet(20L, 8, 0.0)));

        ArgumentCaptor<Set> sets = ArgumentCaptor.forClass(Set.class);
        verify(setRepository, times(2)).save(sets.capture());
        assertThat(sets.getAllValues())
            .extracting(Set::getExerciseId, Set::getPosition)
            .containsExactly(tuple(21L, 0), tuple(20L, 1));
        verify(eventPublisher).publishEvent(new WorkoutLoggedEvent(10L, "user-1", endedAt));
    }

    @Test
    void finishWorkoutRejectsAnEndBeforeTheStartAndSavesNothing() {
        Instant startedAt = Instant.parse("2026-06-01T10:00:00Z");

        assertThatThrownBy(() -> workoutService.finishWorkout("user-1", request(startedAt, startedAt.minusSeconds(1),
                new FinishWorkoutRequest.FinishedSet(20L, 8, 0.0))))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("endedAt must not be before startedAt");
        verify(workoutRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void finishWorkoutRejectsAnotherUsersExerciseAndSavesNothing() {
        Instant endedAt = Instant.parse("2026-06-01T10:00:00Z");
        when(exerciseRepository.findAllById(List.of(20L))).thenReturn(List.of(exercise(20L, "someone-else")));

        assertThatThrownBy(() -> workoutService.finishWorkout("user-1", request(endedAt.minus(Duration.ofHours(1)), endedAt,
                new FinishWorkoutRequest.FinishedSet(20L, 8, 0.0))))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Unknown exercise");
        verify(workoutRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private static FinishWorkoutRequest request(Instant startedAt, Instant endedAt, FinishWorkoutRequest.FinishedSet... sets) {
        return new FinishWorkoutRequest(UUID.randomUUID(), startedAt, endedAt, null, List.of(sets));
    }

    private static Exercise exercise(Long id, String userId) {
        Exercise exercise = new Exercise();
        exercise.setId(id);
        exercise.setUserId(userId);
        return exercise;
    }
}
