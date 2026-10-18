package com.calitrack.controller;

import com.calitrack.projection.WeeklyVolumeView;
import com.calitrack.projection.WorkoutFrequencyView;
import com.calitrack.service.CurrentUserService;
import com.calitrack.service.AnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.calitrack.projection.HeaviestPrView;
import com.calitrack.projection.SetVolumePrView;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUserService;

    public AnalyticsController(AnalyticsService analyticsService, CurrentUserService currentUserService) {
        this.analyticsService = analyticsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/weekly-volume")
    public List<WeeklyVolumeView> weeklyVolume(
            @RequestParam LocalDate weekStart,
            @RequestParam String timeZone,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = currentUserService.getUserId(jwt);
        return analyticsService.getWeeklyVolume(weekStart, parseTimeZone(timeZone), userId);
    }

    @GetMapping("/prs/heaviest")
    public List<HeaviestPrView> heaviestPrs(@AuthenticationPrincipal Jwt jwt) {
        String userId = currentUserService.getUserId(jwt);
        return analyticsService.getHeaviestPrs(userId);
    }
    @GetMapping("/prs/best-set-volume")
    public List<SetVolumePrView> setMaxVolume(@AuthenticationPrincipal Jwt jwt) {
        String userId = currentUserService.getUserId(jwt);
        return analyticsService.getMaxSetVolume(userId);
    }
    @GetMapping("/workout-frequency")
    public List<WorkoutFrequencyView> workoutFrequency(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end,
            @RequestParam String timeZone,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = currentUserService.getUserId(jwt);
        return analyticsService.getWorkoutFrequency(start, end, parseTimeZone(timeZone), userId);
    }

    // The zone the user's weeks are counted in, such as Europe/London. Parsed here rather than
    // bound as a ZoneId, because Spring lets an unknown zone through as a 500.
    private static ZoneId parseTimeZone(String timeZone) {
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timeZone has an invalid value");
        }
    }

}
