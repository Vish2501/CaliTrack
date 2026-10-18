package com.calitrack.integration;

import com.calitrack.entity.Exercise;
import com.calitrack.entity.Set;
import com.calitrack.entity.Workout;
import com.calitrack.repository.ExerciseRepository;
import com.calitrack.repository.SetRepository;
import com.calitrack.repository.WorkoutRepository;
import com.calitrack.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 2026-06-01 is a Monday, and the UK is on summer time (UTC+1) then.
class AnalyticsApiIntegrationTest extends AbstractIntegrationTest {

    // 23:30 on Sunday in UTC, so grouping weeks in UTC would put it in the week before.
    private static final String MONDAY_00_30_LONDON = "2026-06-01T00:30:00+01:00";
    private static final String NEXT_MONDAY_00_00_LONDON = "2026-06-08T00:00:00+01:00";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private SetRepository setRepository;

    @Test
    void workoutJustAfterMondayMidnightInUkSummerTimeCountsInThatMondaysWeek() throws Exception {
        String user = "frequency-monday-user";
        saveWorkout(user, MONDAY_00_30_LONDON);

        mockMvc.perform(get("/api/v1/analytics/workout-frequency")
                .param("start", "2026-05-25")
                .param("end", "2026-06-07")
                .param("timeZone", "Europe/London")
                .with(jwt().jwt(j -> j.subject(user))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].weekStart").value("2026-06-01"))
            .andExpect(jsonPath("$[0].workoutCount").value(1));
    }

    @Test
    void workoutFrequencyExcludesAWorkoutAtMidnightAfterTheEndDate() throws Exception {
        String user = "frequency-end-user";
        saveWorkout(user, MONDAY_00_30_LONDON);
        saveWorkout(user, NEXT_MONDAY_00_00_LONDON);

        mockMvc.perform(get("/api/v1/analytics/workout-frequency")
                .param("start", "2026-06-01")
                .param("end", "2026-06-07")
                .param("timeZone", "Europe/London")
                .with(jwt().jwt(j -> j.subject(user))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].weekStart").value("2026-06-01"));
    }

    @Test
    void weeklyVolumeExcludesAWorkoutAtTheNextMondayMidnight() throws Exception {
        String user = "volume-user";
        Exercise squat = saveExercise(user, "Squat");
        saveSet(saveWorkout(user, MONDAY_00_30_LONDON), squat, 10, 20.0);
        saveSet(saveWorkout(user, NEXT_MONDAY_00_00_LONDON), squat, 5, 100.0);

        mockMvc.perform(get("/api/v1/analytics/weekly-volume")
                .param("weekStart", "2026-06-01")
                .param("timeZone", "Europe/London")
                .with(jwt().jwt(j -> j.subject(user))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].exerciseName").value("Squat"))
            .andExpect(jsonPath("$[0].totalVolume").value(200.0));
    }

    @Test
    void missingTimeZoneIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/workout-frequency")
                .param("start", "2026-06-01")
                .param("end", "2026-06-07")
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void invalidTimeZoneIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/weekly-volume")
                .param("weekStart", "2026-06-01")
                .param("timeZone", "Mars/Olympus_Mons")
                .with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("timeZone has an invalid value"));
    }

    private Workout saveWorkout(String userId, String startTime) {
        Workout workout = new Workout();
        workout.setUserId(userId);
        workout.setStartTime(OffsetDateTime.parse(startTime).toInstant());
        workout.setEndTime(workout.getStartTime().plusSeconds(3600));
        return workoutRepository.saveAndFlush(workout);
    }

    private Exercise saveExercise(String userId, String name) {
        Exercise exercise = new Exercise();
        exercise.setUserId(userId);
        exercise.setName(name);
        return exerciseRepository.saveAndFlush(exercise);
    }

    private void saveSet(Workout workout, Exercise exercise, int reps, double weight) {
        Set set = new Set();
        set.setWorkoutId(workout.getId());
        set.setExerciseId(exercise.getId());
        set.setReps(reps);
        set.setWeight(weight);
        set.setTimestamp(workout.getStartTime());
        setRepository.saveAndFlush(set);
    }
}
