package com.example.proyectolp.service;

import java.time.LocalDate;
import java.util.Set;

public class ProductivityStreakService {
    public int calculate(Set<LocalDate> activityDates, LocalDate today) {
        if (!activityDates.contains(today) && !activityDates.contains(today.minusDays(1))) return 0;
        LocalDate cursor = activityDates.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (activityDates.contains(cursor)) { streak++; cursor = cursor.minusDays(1); }
        return streak;
    }
}
