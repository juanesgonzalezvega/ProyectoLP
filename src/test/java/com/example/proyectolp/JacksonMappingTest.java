package com.example.proyectolp;

import com.example.proyectolp.model.Project;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JacksonMappingTest {
    @Test void mapsSnakeCaseProjectFields() throws Exception {
        Project project = new ObjectMapper().readValue("{\"id\":7,\"name\":\"Semestre\",\"main_goal\":\"Terminar\"}", Project.class);
        assertEquals(7, project.getId());
        assertEquals("Terminar", project.getMainGoal());
    }
}
