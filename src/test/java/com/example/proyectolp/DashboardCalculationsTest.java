package com.example.proyectolp;

import com.example.proyectolp.model.Stage;
import com.example.proyectolp.service.ProjectProgressService;
import org.junit.jupiter.api.Test;
import java.util.List;
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
    private static Stage stage(String status) { Stage s = new Stage(); s.setStatus(status); return s; }
}
