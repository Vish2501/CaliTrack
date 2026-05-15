package com.calitrack.service;

import com.calitrack.entity.Exercise;
import com.calitrack.repository.ExerciseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @InjectMocks
    private ExerciseService exerciseService;

    @Test
    void deleteExerciseRemovesOwnedExercise() {
        Exercise exercise = new Exercise();
        exercise.setId(1L);
        exercise.setUserId("user-1");

        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));

        exerciseService.deleteExercise(1L, "user-1");

        verify(exerciseRepository).deleteById(1L);
    }

    @Test
    void deleteExerciseThrowsNotFoundWhenMissing() {
        when(exerciseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exerciseService.deleteExercise(99L, "user-1"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Exercise not found");
    }

    @Test
    void deleteExerciseThrowsForbiddenForOtherUser() {
        Exercise exercise = new Exercise();
        exercise.setId(1L);
        exercise.setUserId("owner");

        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));

        assertThatThrownBy(() -> exerciseService.deleteExercise(1L, "other-user"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Forbidden");
    }
}
