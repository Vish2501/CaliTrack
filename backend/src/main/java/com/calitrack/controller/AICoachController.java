package com.calitrack.controller;

import com.calitrack.dto.CoachRecommendationRequest;
import com.calitrack.dto.CoachRecommendationResponse;
import com.calitrack.service.AICoachService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coach")
public class AICoachController {

    private final AICoachService aiCoachService;

    public AICoachController(AICoachService aiCoachService) {
        this.aiCoachService = aiCoachService;
    }

    /**
     * Get AI-powered coaching recommendations based on recent workout history
     * 
     * @param request The recommendation request with workout history and user goals
     * @param jwt     The authenticated user's JWT token
     * @return AI-generated coaching recommendation
     */
    @PostMapping("/recommend")
    public ResponseEntity<CoachRecommendationResponse> getRecommendation(
            @RequestBody CoachRecommendationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        CoachRecommendationResponse recommendation = aiCoachService.getRecommendation(request);
        return ResponseEntity.ok(recommendation);
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
            @RequestParam(defaultValue = "5") int numWorkouts,
            @RequestParam(defaultValue = "general fitness") String userGoals,
            @RequestParam(defaultValue = "intermediate") String fitnessLevel,
            @AuthenticationPrincipal Jwt jwt) {
        CoachRecommendationResponse recommendation = aiCoachService.getAutoRecommendation(
                jwt.getSubject(),
                numWorkouts,
                userGoals,
                fitnessLevel);
        return ResponseEntity.ok(recommendation);
    }
}
