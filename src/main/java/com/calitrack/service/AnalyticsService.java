package com.calitrack.service;

import com.calitrack.projection.WeeklyVolumeView;
import com.calitrack.projection.WorkoutFrequencyView;
import com.calitrack.repository.SetRepository;
import com.calitrack.repository.WorkoutRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public List<WeeklyVolumeView> getWeeklyVolume(LocalDate weekStart, String userId) {
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(7).atStartOfDay();

        return setRepository.findWeeklyVolume(userId, start, end);
    }

    public List<HeaviestPrView> getHeaviestPrs(String userId) {
        return setRepository.findHeaviestPrs(userId);
    }

    public List<SetVolumePrView> getMaxSetVolume(String userId){
        return setRepository.findMaxSetVolume(userId);
    }

    public List<WorkoutFrequencyView> getWorkoutFrequency(LocalDate start, LocalDate end, String userId) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.plusDays(1).atStartOfDay(); // include end date
        return workoutRepository.findWorkoutFrequency(userId, startDt, endDt);
    }
}
