package com.example.proyectolp;

import com.example.proyectolp.model.TaskItem;
import com.example.proyectolp.service.TaskValidationService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TaskValidationTest {
    private final TaskValidationService validation = new TaskValidationService();
    @Test void requiresNamePriorityAndStatus() {
        TaskItem task = new TaskItem(); task.setPriority("media"); task.setStatus("pendiente");
        assertEquals("El nombre es obligatorio.", validation.validate(task, true, LocalDate.now()));
        task.setName("Leer"); task.setPriority(null);
        assertEquals("Selecciona una prioridad válida.", validation.validate(task, true, LocalDate.now()));
    }
    @Test void rejectsPastDueDateOnlyWhenCreating() {
        TaskItem task = new TaskItem(); task.setName("Tarea"); task.setPriority("alta"); task.setStatus("pendiente"); task.setDueDate(LocalDate.of(2025, 1, 1));
        assertEquals("La fecha límite no puede ser anterior a hoy al crear una tarea.", validation.validate(task, true, LocalDate.of(2026, 1, 1)));
        assertNull(validation.validate(task, false, LocalDate.of(2026, 1, 1)));
    }
}
