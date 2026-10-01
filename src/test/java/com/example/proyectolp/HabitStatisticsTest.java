package com.example.proyectolp;

import com.example.proyectolp.model.HabitLog;
import com.example.proyectolp.service.HabitStatisticsService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HabitStatisticsTest {
    private final HabitStatisticsService service = new HabitStatisticsService();
    private final LocalDate today = LocalDate.of(2026, 9, 25);

    @Test void currentStreakIncludesConsecutiveDaysThroughTodayOrYesterdayAndBreaksAtGap() {
        assertEquals(3, service.calculate(logs(today, 0, 1, 2, 4), today).currentStreak());
        assertEquals(2, service.calculate(logs(today, 1, 2), today).currentStreak());
        assertEquals(0, service.calculate(logs(today, 2, 4), today).currentStreak());
    }

    @Test void bestStreakFindsLongestHistoricalRun() {
        assertEquals(4, service.calculate(logs(today, 10, 11, 12, 13, 2, 0), today).bestStreak());
        assertEquals(0, service.calculate(List.of(), today).bestStreak());
    }

    @Test void complianceUsesThirtyDaysIncludingToday() {
        List<HabitLog> logs = logs(today, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
        assertEquals(50, service.calculate(logs, today).compliancePercent());
    }

    private List<HabitLog> logs(LocalDate end, int... daysBefore) {
        List<HabitLog> values = new ArrayList<>();
        for (int days : daysBefore) {
            HabitLog log = new HabitLog(); log.setLogDate(end.minusDays(days)); log.setCompleted(true); values.add(log);
        }
        return values;
    }
}
