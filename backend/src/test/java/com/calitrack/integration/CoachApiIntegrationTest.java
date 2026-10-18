package com.calitrack.integration;

import com.calitrack.entity.CoachRecommendation;
import com.calitrack.entity.Workout;
import com.calitrack.repository.CoachRecommendationRepository;
import com.calitrack.repository.WorkoutRepository;
import com.calitrack.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CoachApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private CoachRecommendationRepository recommendationRepository;

    @Test
    void profileDefaultsUntilTheUserSetsOne() throws Exception {
        mockMvc.perform(get("/api/v1/coach/profile").with(jwt().jwt(j -> j.subject("new-user"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.goals").value("general fitness"))
            .andExpect(jsonPath("$.fitnessLevel").value("intermediate"));
    }

    @Test
    void updatedProfileIsReturnedOnlyToItsOwner() throws Exception {
        mockMvc.perform(put("/api/v1/coach/profile")
                .with(jwt().jwt(j -> j.subject(TEST_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"goals\":\"build strength\",\"fitnessLevel\":\"advanced\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.goals").value("build strength"));

        mockMvc.perform(get("/api/v1/coach/profile").with(jwt().jwt(j -> j.subject(TEST_USER))))
            .andExpect(jsonPath("$.goals").value("build strength"))
            .andExpect(jsonPath("$.fitnessLevel").value("advanced"));

        mockMvc.perform(get("/api/v1/coach/profile").with(jwt().jwt(j -> j.subject(OTHER_USER))))
            .andExpect(jsonPath("$.goals").value("general fitness"));
    }

    @Test
    void blankProfileFieldsAreRejected() throws Exception {
        mockMvc.perform(put("/api/v1/coach/profile")
                .with(jwt().jwt(j -> j.subject(TEST_USER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"goals\":\" \",\"fitnessLevel\":\"beginner\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void latestIsEmptyBeforeAnyRecommendationExists() throws Exception {
        mockMvc.perform(get("/api/v1/coach/recommendations/latest")
                .with(jwt().jwt(j -> j.subject("no-history-user"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendation").isEmpty())
            .andExpect(jsonPath("$.pending").value(false));
    }

    @Test
    void latestReturnsTheNewestRecommendationForThatUserOnly() throws Exception {
        saveRecommendation("latest-api-user", "Older advice.");
        Workout newest = saveRecommendation("latest-api-user", "Newest advice.");
        saveRecommendation("someone-else", "Not yours.");

        mockMvc.perform(get("/api/v1/coach/recommendations/latest")
                .with(jwt().jwt(j -> j.subject("latest-api-user"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendation.recommendation").value("Newest advice."))
            .andExpect(jsonPath("$.recommendation.workoutId").value(newest.getId()))
            .andExpect(jsonPath("$.recommendation.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.pending").value(false));
    }

    private Workout saveRecommendation(String userId, String text) {
        Workout workout = new Workout();
        workout.setUserId(userId);
        workout.setStartTime(Instant.now().minus(Duration.ofHours(1)));
        workout.setEndTime(Instant.now());
        workout = workoutRepository.save(workout);

        CoachRecommendation recommendation = new CoachRecommendation();
        recommendation.setUserId(userId);
        recommendation.setWorkoutId(workout.getId());
        recommendation.setRecommendation(text);
        recommendation.setFocusArea("Back");
        recommendation.setNextExercise("Pull-Up");
        recommendationRepository.save(recommendation);
        return workout;
    }
}
