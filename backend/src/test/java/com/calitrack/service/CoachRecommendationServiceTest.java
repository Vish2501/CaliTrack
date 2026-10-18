package com.calitrack.service;

import com.calitrack.dto.LatestCoachRecommendationResponse;
import com.calitrack.entity.CoachRecommendation;
import com.calitrack.entity.Workout;
import com.calitrack.repository.CoachRecommendationRepository;
import com.calitrack.repository.WorkoutRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoachRecommendationServiceTest {

    private static final String USER = "user-1";

    @Mock
    private AICoachService aiCoachService;

    @Mock
    private CoachProfileService coachProfileService;

    @Mock
    private CoachRecommendationRepository recommendationRepository;

    @Mock
    private WorkoutRepository workoutRepository;

    @Test
    void getLatestReturnsTheSavedRecommendation() {
        CoachRecommendation saved = new CoachRecommendation();
        saved.setWorkoutId(7L);
        saved.setRecommendation("Add a set of dips.");
        saved.setFocusArea("Triceps");
        saved.setNextExercise("Dip");
        when(recommendationRepository.findFirstByUserIdOrderByIdDesc(USER)).thenReturn(Optional.of(saved));

        LatestCoachRecommendationResponse latest = service(false).getLatest(USER);

        assertThat(latest.recommendation().workoutId()).isEqualTo(7L);
        assertThat(latest.recommendation().suggestedNextExercise()).isEqualTo("Dip");
        assertThat(latest.pending()).isFalse();
    }

    @Test
    void neverPendingWhenAsyncCoachIsOff() {
        when(recommendationRepository.findFirstByUserIdOrderByIdDesc(USER)).thenReturn(Optional.empty());

        assertThat(service(false).getLatest(USER).pending()).isFalse();
    }

    @Test
    void pendingWhenTheLatestFinishedWorkoutHasNoRecommendationYet() {
        Workout workout = finishedWorkout(Instant.now().minusSeconds(10));
        when(recommendationRepository.findFirstByUserIdOrderByIdDesc(USER)).thenReturn(Optional.empty());
        when(workoutRepository.findFirstByUserIdAndEndTimeIsNotNullOrderByEndTimeDesc(USER))
            .thenReturn(Optional.of(workout));
        when(recommendationRepository.existsByWorkoutId(workout.getId())).thenReturn(false);

        assertThat(service(true).getLatest(USER).pending()).isTrue();
    }

    @Test
    void notPendingOnceTheLatestWorkoutHasARecommendation() {
        Workout workout = finishedWorkout(Instant.now().minusSeconds(10));
        when(recommendationRepository.findFirstByUserIdOrderByIdDesc(USER)).thenReturn(Optional.empty());
        when(workoutRepository.findFirstByUserIdAndEndTimeIsNotNullOrderByEndTimeDesc(USER))
            .thenReturn(Optional.of(workout));
        when(recommendationRepository.existsByWorkoutId(workout.getId())).thenReturn(true);

        assertThat(service(true).getLatest(USER).pending()).isFalse();
    }

    @Test
    void notPendingWhenTheWorkoutFinishedLongerAgoThanTheWindow() {
        Workout workout = finishedWorkout(
            Instant.now().minus(CoachRecommendationService.PENDING_WINDOW).minusSeconds(1));
        when(recommendationRepository.findFirstByUserIdOrderByIdDesc(USER)).thenReturn(Optional.empty());
        when(workoutRepository.findFirstByUserIdAndEndTimeIsNotNullOrderByEndTimeDesc(USER))
            .thenReturn(Optional.of(workout));

        assertThat(service(true).getLatest(USER).pending()).isFalse();
    }

    private CoachRecommendationService service(boolean asyncEnabled) {
        return new CoachRecommendationService(
            aiCoachService, coachProfileService, recommendationRepository, workoutRepository, asyncEnabled);
    }

    private static Workout finishedWorkout(Instant endTime) {
        Workout workout = new Workout();
        workout.setId(42L);
        workout.setUserId(USER);
        workout.setStartTime(endTime.minus(Duration.ofHours(1)));
        workout.setEndTime(endTime);
        return workout;
    }
}
