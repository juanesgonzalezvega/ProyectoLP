package com.example.proyectolp.service;

import com.example.proyectolp.model.HabitLog;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

public class HabitStatisticsService {
    public record Statistics(int currentStreak, int bestStreak, int compliancePercent) { }

    public Statistics calculate(Collection<HabitLog> logs, LocalDate today) {
        Set<LocalDate> completedDates = new TreeSet<>();
        for (HabitLog log : logs) if (log.isCompleted() && log.getLogDate() != null) completedDates.add(log.getLogDate());
        return new Statistics(currentStreak(completedDates, today), bestStreak(completedDates), compliance(completedDates, today));
    }

    public int currentStreak(Set<LocalDate> completedDates, LocalDate today) {
        LocalDate cursor;
        if (completedDates.contains(today)) cursor = today;
        else if (completedDates.contains(today.minusDays(1))) cursor = today.minusDays(1);
        else return 0;
        int count = 0;
        while (completedDates.contains(cursor)) { count++; cursor = cursor.minusDays(1); }
        return count;
    }

    public int bestStreak(Collection<LocalDate> completedDates) {
        int best = 0, current = 0;
        LocalDate previous = null;
        for (LocalDate date : new TreeSet<>(completedDates)) {
            if (previous != null && date.equals(previous.plusDays(1))) current++;
            else current = 1;
            best = Math.max(best, current); previous = date;
        }
        return best;
    }

    public int compliance(Set<LocalDate> completedDates, LocalDate today) {
        LocalDate from = today.minusDays(29);
        long successes = completedDates.stream().filter(date -> !date.isBefore(from) && !date.isAfter(today)).count();
        return (int) Math.round(successes * 100.0 / 30.0);
    }
}
