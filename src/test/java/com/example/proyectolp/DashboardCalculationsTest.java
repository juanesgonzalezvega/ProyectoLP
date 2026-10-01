package com.example.proyectolp;

import com.example.proyectolp.model.Stage;
import com.example.proyectolp.service.ProjectProgressService;
import com.example.proyectolp.service.ProductivityStreakService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DashboardCalculationsTest {
    @Test void calculatesCompletedStagePercentage() {
        Stage a = stage("completada"); Stage b = stage("pendiente");
        ProjectProgressService.Progress result = new ProjectProgressService().calculate(List.of(a, b));
        assertEquals(1, result.completed()); assertEquals(2, result.total()); assertEquals(50, result.percent());
    }
    @Test void representsProjectWithoutStages() {
        ProjectProgressService.Progress result = new ProjectProgressService().calculate(List.of());
        assertFalse(result.hasStages()); assertEquals(0, result.percent());
    }
    @Test void calculatesCurrentAndExpiredStreaks() {
        LocalDate today = LocalDate.of(2026, 9, 24);
        ProductivityStreakService service = new ProductivityStreakService();
        assertEquals(3, service.calculate(Set.of(today, today.minusDays(1), today.minusDays(2)), today));
        assertEquals(0, service.calculate(Set.of(today.minusDays(2)), today));
    }
    private static Stage stage(String status) { Stage s = new Stage(); s.setStatus(status); return s; }
}
