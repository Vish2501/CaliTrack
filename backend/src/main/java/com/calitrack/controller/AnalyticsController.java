package com.calitrack.controller;

import com.calitrack.projection.WeeklyVolumeView;
import com.calitrack.projection.WorkoutFrequencyView;
import com.calitrack.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.calitrack.projection.HeaviestPrView;
import com.calitrack.projection.SetVolumePrView;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/weekly-volume")
    public List<WeeklyVolumeView> weeklyVolume(
            @RequestParam LocalDate weekStart,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        return analyticsService.getWeeklyVolume(weekStart, userId);
    }

    @GetMapping("/prs/heaviest")
    public List<HeaviestPrView> heaviestPrs(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return analyticsService.getHeaviestPrs(userId);
    }
    @GetMapping("/prs/best-set-volume")
    public List<SetVolumePrView> setMaxVolume(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return analyticsService.getMaxSetVolume(userId);
    }
    @GetMapping("/workout-frequency")
    public List<WorkoutFrequencyView> workoutFrequency(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        return analyticsService.getWorkoutFrequency(start, end, userId);
    }

}
