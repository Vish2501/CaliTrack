package com.calitrack.controller;

import com.calitrack.dto.CoachProfileResponse;
import com.calitrack.dto.CoachRecommendationResponse;
import com.calitrack.dto.LatestCoachRecommendationResponse;
import com.calitrack.dto.UpdateCoachProfileRequest;
import com.calitrack.service.AICoachService;
import com.calitrack.service.CoachProfileService;
import com.calitrack.service.CoachRecommendationService;
import com.calitrack.service.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coach")
@Validated
public class AICoachController {

    private final AICoachService aiCoachService;
    private final CoachRecommendationService coachRecommendationService;
    private final CoachProfileService coachProfileService;
    private final CurrentUserService currentUserService;

    public AICoachController(AICoachService aiCoachService,
                             CoachRecommendationService coachRecommendationService,
                             CoachProfileService coachProfileService,
                             CurrentUserService currentUserService) {
        this.aiCoachService = aiCoachService;
        this.coachRecommendationService = coachRecommendationService;
        this.coachProfileService = coachProfileService;
        this.currentUserService = currentUserService;
    }

    /**
     * Get AI coaching recommendation by automatically fetching recent workouts
     * 
     * @param numWorkouts  Number of recent completed workouts to analyze (default:
     *                     5)
     * @param userGoals    User's fitness goals (e.g., "strength", "endurance",
     *                     "hypertrophy")
     * @param fitnessLevel User's fitness level (e.g., "beginner", "intermediate",
     *                     "advanced")
     * @param jwt          The authenticated user's JWT token
     * @return AI-generated coaching recommendation
     */
    @GetMapping("/recommend")
    public ResponseEntity<CoachRecommendationResponse> getAutoRecommendation(
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int numWorkouts,
            @RequestParam(defaultValue = "general fitness") @Size(max = 200) String userGoals,
            @RequestParam(defaultValue = "intermediate") @Size(max = 50) String fitnessLevel,
            @AuthenticationPrincipal Jwt jwt) {
        CoachRecommendationResponse recommendation = aiCoachService.getAutoRecommendation(
                currentUserService.getUserId(jwt),
                numWorkouts,
                userGoals,
                fitnessLevel);
        return ResponseEntity.ok(recommendation);
    }

    /**
     * Get the latest recommendation generated in the background after a finished workout
     *
     * @param jwt The authenticated user's JWT token
     * @return The latest saved recommendation (null if none) and whether a newer one is pending
     */
    @GetMapping("/recommendations/latest")
    public ResponseEntity<LatestCoachRecommendationResponse> getLatestRecommendation(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(coachRecommendationService.getLatest(currentUserService.getUserId(jwt)));
    }

    /**
     * Get the goals and fitness level used for background recommendations
     *
     * @param jwt The authenticated user's JWT token
     * @return The saved profile, or the defaults if none has been set
     */
    @GetMapping("/profile")
    public ResponseEntity<CoachProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(coachProfileService.getProfile(currentUserService.getUserId(jwt)));
    }

    /**
     * Set the goals and fitness level used for background recommendations
     *
     * @param request The new goals and fitness level
     * @param jwt     The authenticated user's JWT token
     * @return The saved profile
     */
    @PutMapping("/profile")
    public ResponseEntity<CoachProfileResponse> updateProfile(
            @Valid @RequestBody UpdateCoachProfileRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(coachProfileService.updateProfile(currentUserService.getUserId(jwt), request));
    }
}
