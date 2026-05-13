package com.calitrack.projection;

import java.time.LocalDate;

public interface WorkoutFrequencyView {

    LocalDate getWeekStart();
    Long getWorkoutCount();
    
}
