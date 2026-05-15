package com.calitrack.service;

import com.calitrack.dto.WorkoutDetailsResponse;
import com.calitrack.entity.Exercise;
import com.calitrack.entity.Set;
import com.calitrack.entity.Workout;
import com.calitrack.repository.ExerciseRepository;
import com.calitrack.repository.SetRepository;
import com.calitrack.repository.WorkoutRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private WorkoutService workoutService;

    @Test
    void startWorkoutPersistsWorkoutForUser() {
        when(workoutRepository.save(any(Workout.class))).thenAnswer(invocation -> {
            Workout workout = invocation.getArgument(0);
            workout.setId(1L);
            return workout;
        });

        Workout result = workoutService.startWorkout("user-1");

        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getStartTime()).isNotNull();
        verify(workoutRepository).save(any(Workout.class));
    }

    @Test
    void getWorkoutDetailsReturnsSetsWithExerciseNames() {
        Workout workout = new Workout();
        workout.setId(10L);
        workout.setUserId("user-1");
        workout.setStartTime(LocalDateTime.now());

        Set set = new Set();
        set.setId(5L);
        set.setWorkoutId(10L);
        set.setExerciseId(20L);
        set.setReps(12);
        set.setWeight(0.0);
        set.setRpe(7.0);
        set.setTimestamp(LocalDateTime.now());

        Exercise exercise = new Exercise();
        exercise.setId(20L);
        exercise.setName("Pull-Up");

        when(workoutRepository.findByIdAndUserId(10L, "user-1")).thenReturn(Optional.of(workout));
        when(setRepository.findByWorkoutId(10L)).thenReturn(List.of(set));
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
}
