package com.example.proyectolp.service;

import com.example.proyectolp.model.TaskItem;
import java.time.LocalDate;

public class TaskValidationService {
    public String validate(TaskItem task, boolean creating, LocalDate today) {
        if (task.getName() == null || task.getName().isBlank()) return "El nombre es obligatorio.";
        if (!in(task.getPriority(), "baja", "media", "alta")) return "Selecciona una prioridad válida.";
        if (!in(task.getStatus(), "pendiente", "en_progreso", "completada")) return "Selecciona un estado válido.";
        if (task.getProgress() < 0 || task.getProgress() > 100) return "El progreso debe estar entre 0 y 100.";
        if (creating && task.getDueDate() != null && task.getDueDate().isBefore(today)) return "La fecha límite no puede ser anterior a hoy al crear una tarea.";
        return null;
    }
    private boolean in(String value, String... allowed) { for (String option : allowed) if (option.equals(value)) return true; return false; }
}
