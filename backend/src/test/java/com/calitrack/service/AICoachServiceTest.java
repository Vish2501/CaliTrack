package com.calitrack.service;

import com.calitrack.dto.CoachRecommendationRequest;
import com.calitrack.dto.CoachRecommendationResponse;
import com.calitrack.entity.Exercise;
import com.calitrack.entity.Set;
import com.calitrack.entity.Workout;
import com.calitrack.repository.ExerciseRepository;
import com.calitrack.repository.SetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AICoachServiceTest {

    @Mock
    private OpenAIChatClient openAIChatClient;

    @Mock
    private WorkoutService workoutService;

    @Mock
    private SetRepository setRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    private AICoachService aiCoachService;

    @BeforeEach
    void setUp() {
        aiCoachService = new AICoachService(
            openAIChatClient,
            workoutService,
            setRepository,
            exerciseRepository
        );
    }

    @Test
    void getRecommendationThrowsWhenApiKeyMissing() {
        when(openAIChatClient.isConfigured()).thenReturn(false);

        CoachRecommendationRequest request = new CoachRecommendationRequest(
            List.of(),
            "strength",
            "intermediate"
        );

        assertThatThrownBy(() -> aiCoachService.getRecommendation(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("missing OPENAI_API_KEY");
    }

    @Test
    void getRecommendationParsesOpenAiResponse() {
        when(openAIChatClient.isConfigured()).thenReturn(true);
        when(openAIChatClient.complete(anyString())).thenReturn("""
            RECOMMENDATION: Add more pulling volume this week.
            FOCUS_AREA: Back and biceps
            NEXT_EXERCISE: Chin-Up
            """);

        CoachRecommendationResponse response = aiCoachService.getRecommendation(
            new CoachRecommendationRequest(List.of(), "strength", "intermediate")
        );

        assertThat(response.recommendation()).contains("pulling volume");
        assertThat(response.focusArea()).isEqualTo("Back and biceps");
        assertThat(response.suggestedNextExercise()).isEqualTo("Chin-Up");
    }

    @Test
    void getAutoRecommendationBuildsRequestFromRecentCompletedWorkouts() {
        Workout completedWorkout = new Workout();
        completedWorkout.setId(1L);
        completedWorkout.setUserId("user-1");
        completedWorkout.setStartTime(LocalDateTime.of(2026, 5, 20, 9, 0));
        completedWorkout.setEndTime(LocalDateTime.of(2026, 5, 20, 10, 0));

        Workout activeWorkout = new Workout();
        activeWorkout.setId(2L);
        activeWorkout.setUserId("user-1");
        activeWorkout.setStartTime(LocalDateTime.of(2026, 5, 21, 9, 0));

        Exercise exercise = new Exercise();
        exercise.setId(10L);
        exercise.setName("Pull-Up");
        exercise.setCategory("Back");
        exercise.setUserId("user-1");

        Set firstSet = new Set();
        firstSet.setWorkoutId(1L);
        firstSet.setExerciseId(10L);
        firstSet.setReps(8);
        firstSet.setWeight(0.0);
        firstSet.setRpe(8.0);

        Set secondSet = new Set();
        secondSet.setWorkoutId(1L);
        secondSet.setExerciseId(10L);
        secondSet.setReps(10);
        secondSet.setWeight(0.0);
        secondSet.setRpe(9.0);

        when(workoutService.getWorkoutsForUser("user-1")).thenReturn(List.of(activeWorkout, completedWorkout));
        when(setRepository.findByWorkoutId(1L)).thenReturn(List.of(firstSet, secondSet));
        when(exerciseRepository.findAllById(List.of(10L))).thenReturn(List.of(exercise));
        when(openAIChatClient.isConfigured()).thenReturn(true);
        when(openAIChatClient.complete(anyString())).thenReturn("""
            RECOMMENDATION: Keep progressing your pulling work.
            FOCUS_AREA: Back
            NEXT_EXERCISE: Pull-Up
            """);

        CoachRecommendationResponse response = aiCoachService.getAutoRecommendation(
            "user-1",
            5,
            "strength",
            "intermediate"
        );

        assertThat(response.recommendation()).contains("pulling work");
        assertThat(response.focusArea()).isEqualTo("Back");
        assertThat(response.suggestedNextExercise()).isEqualTo("Pull-Up");
    }
}
