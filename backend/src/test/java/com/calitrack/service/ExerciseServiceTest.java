package com.calitrack.service;

import com.calitrack.dto.CreateExerciseRequest;
import com.calitrack.entity.Exercise;
import com.calitrack.repository.ExerciseRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @InjectMocks
    private ExerciseService exerciseService;

    @Test
    void deleteExerciseArchivesOwnedExercise() {
        Exercise exercise = exercise(1L, "user-1", "Pull-Up", false);
        when(exerciseRepository.findByIdAndUserId(1L, "user-1")).thenReturn(Optional.of(exercise));

        exerciseService.deleteExercise(1L, "user-1");

        assertThat(exercise.isArchived()).isTrue();
        verify(exerciseRepository).save(exercise);
    }

    @Test
    void deleteExerciseThrowsNotFoundWhenMissingOrNotOwned() {
        when(exerciseRepository.findByIdAndUserId(99L, "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exerciseService.deleteExercise(99L, "user-1"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Exercise not found");
    }

    @Test
    void createExerciseRestoresArchivedExerciseWithTheSameName() {
        Exercise archived = exercise(1L, "user-1", "Pull-Up", true);
        when(exerciseRepository.findByUserIdAndNameIgnoreCase("user-1", "pull-up")).thenReturn(Optional.of(archived));
        when(exerciseRepository.save(archived)).thenReturn(archived);

        Exercise result = exerciseService.createExercise(request("pull-up"), "user-1");

        assertThat(result).isSameAs(archived);
        assertThat(result.isArchived()).isFalse();
        verify(exerciseRepository, never()).saveAndFlush(any());
    }

    @Test
    void createExerciseRejectsTheNameOfAnActiveExercise() {
        Exercise active = exercise(1L, "user-1", "Dip", false);
        when(exerciseRepository.findByUserIdAndNameIgnoreCase("user-1", "Dip")).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> exerciseService.createExercise(request("Dip"), "user-1"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("already exists");
    }

    private static Exercise exercise(Long id, String userId, String name, boolean archived) {
        Exercise exercise = new Exercise();
        exercise.setId(id);
        exercise.setUserId(userId);
        exercise.setName(name);
        exercise.setArchived(archived);
        return exercise;
    }

    private static CreateExerciseRequest request(String name) {
        CreateExerciseRequest request = new CreateExerciseRequest();
        request.setName(name);
        return request;
    }
}
