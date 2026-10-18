package com.calitrack.service;

import com.calitrack.projection.WeeklyVolumeView;
import com.calitrack.projection.WorkoutFrequencyView;
import com.calitrack.repository.SetRepository;
import com.calitrack.repository.WorkoutRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import com.calitrack.projection.HeaviestPrView;
import com.calitrack.projection.SetVolumePrView;

@Service
public class AnalyticsService {

    private final SetRepository setRepository;
    private final WorkoutRepository workoutRepository;

    public AnalyticsService(SetRepository setRepository, WorkoutRepository workoutRepository) {
        this.setRepository = setRepository;
        this.workoutRepository = workoutRepository;
    }

    // Days start at midnight in the user's time zone. Ranges are half-open: the end moment is excluded.
    public List<WeeklyVolumeView> getWeeklyVolume(LocalDate weekStart, ZoneId timeZone, String userId) {
        Instant start = weekStart.atStartOfDay(timeZone).toInstant();
        Instant end = weekStart.plusDays(7).atStartOfDay(timeZone).toInstant();

        return setRepository.findWeeklyVolume(userId, start, end);
    }

    public List<HeaviestPrView> getHeaviestPrs(String userId) {
        return setRepository.findHeaviestPrs(userId);
    }

    public List<SetVolumePrView> getMaxSetVolume(String userId){
        return setRepository.findMaxSetVolume(userId);
    }

    public List<WorkoutFrequencyView> getWorkoutFrequency(LocalDate start, LocalDate end, ZoneId timeZone, String userId) {
        Instant from = start.atStartOfDay(timeZone).toInstant();
        Instant until = end.plusDays(1).atStartOfDay(timeZone).toInstant(); // the end date is included
        return workoutRepository.findWorkoutFrequency(userId, from, until, timeZone.getId());
    }
}
