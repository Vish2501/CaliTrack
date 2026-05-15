package com.calitrack.service;

import com.calitrack.dto.CreateSetRequest;
import com.calitrack.dto.SetResponse;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetServiceTest {

    @Mock
    private SetRepository setRepository;

    @Mock
    private WorkoutRepository workoutRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @InjectMocks
    private SetService setService;

    @Test
    void addSetReturnsSetResponse() {
        Workout workout = new Workout();
        workout.setId(1L);

        Exercise exercise = new Exercise();
        exercise.setId(2L);
        exercise.setName("Squat");

        Set saved = new Set();
        saved.setId(3L);
        saved.setWorkoutId(1L);
        saved.setExerciseId(2L);
        saved.setReps(5);
        saved.setWeight(135.0);
        saved.setRpe(8.0);
        saved.setTimestamp(java.time.LocalDateTime.now());

        when(workoutRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(workout));
        when(exerciseRepository.findByIdAndUserId(2L, "user-1")).thenReturn(Optional.of(exercise));
        when(setRepository.save(any(Set.class))).thenReturn(saved);

        SetResponse response = setService.addSet(
            1L,
            "user-1",
            new CreateSetRequest(2L, 5, 135.0, 8.0, null)
        );

        assertThat(response.exerciseName()).isEqualTo("Squat");
        assertThat(response.reps()).isEqualTo(5);
    }

    @Test
    void addSetThrowsNotFoundWhenWorkoutMissing() {
        when(workoutRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> setService.addSet(
            1L,
            "user-1",
            new CreateSetRequest(2L, 5, 135.0, 8.0, null)
        ))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Workout not found");
    }

    @Test
    void addSetThrowsNotFoundWhenExerciseMissing() {
        Workout workout = new Workout();
        workout.setId(1L);

        when(workoutRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(workout));
        when(exerciseRepository.findByIdAndUserId(2L, "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> setService.addSet(
            1L,
            "user-1",
            new CreateSetRequest(2L, 5, 135.0, 8.0, null)
        ))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Exercise not found");
    }
}
