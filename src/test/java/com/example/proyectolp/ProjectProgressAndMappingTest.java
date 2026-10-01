package com.example.proyectolp;

import com.example.proyectolp.model.Project;
import com.example.proyectolp.model.Stage;
import com.example.proyectolp.service.ProjectProgressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectProgressAndMappingTest {
    @Test void calculatesEmptyZeroPartialAndCompleteProgress() {
        ProjectProgressService service = new ProjectProgressService();
        assertEquals("Sin etapas", service.display(List.of()));
        assertEquals(0, service.calculate(List.of(stage("pendiente"), stage("en_progreso"))).percent());
        assertEquals(50, service.calculate(List.of(stage("completada"), stage("pendiente"))).percent());
        assertEquals(100, service.calculate(List.of(stage("completada"), stage("completada"))).percent());
    }

    @Test void mapsProjectAndStageSnakeCaseFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Project project = mapper.readValue("{\"id\":4,\"name\":\"Semestre\",\"main_goal\":\"Aprobar\",\"deadline\":\"2026-12-01\"}", Project.class);
        Stage stage = mapper.readValue("{\"id\":8,\"project_id\":4,\"name\":\"Diseño\",\"position\":2,\"status\":\"completada\"}", Stage.class);
        assertEquals(4, project.getId()); assertEquals("Aprobar", project.getMainGoal());
        assertEquals(LocalDate.of(2026, 12, 1), project.getDeadline()); assertEquals(4, stage.getProjectId());
        assertTrue("completada".equals(stage.getStatus()));
    }
    private static Stage stage(String status) { Stage stage = new Stage(); stage.setStatus(status); return stage; }
}
