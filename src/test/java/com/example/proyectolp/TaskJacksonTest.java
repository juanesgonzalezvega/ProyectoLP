package com.example.proyectolp;

import com.example.proyectolp.model.TaskItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskJacksonTest {
    @Test void mapsTaskFieldsAndDatesInSnakeCase() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        TaskItem source = new TaskItem(); source.setName("Entregar informe"); source.setPriority("alta"); source.setStatus("pendiente");
        source.setDueDate(LocalDate.of(2026, 10, 5)); source.setProgress(20); source.setProjectId(9L);
        String json = mapper.writeValueAsString(source);
        assertTrue(json.contains("\"due_date\":\"2026-10-05\""));
        TaskItem copy = mapper.readValue("{\"name\":\"Entregar informe\",\"due_date\":\"2026-10-05\",\"project_id\":9,\"progress\":20}", TaskItem.class);
        assertEquals(source.getDueDate(), copy.getDueDate()); assertEquals(9L, copy.getProjectId()); assertEquals(20, copy.getProgress());
    }
}
