package com.example.proyectolp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class TrainingModels {
    private TrainingModels() {}
    public record Routine(Long id, String name, String description, List<Exercise> exercises) {}
    public record Exercise(Long id, Long routineId, String name, String muscleGroup, int position) {}
    public record Session(Long id, Long routineId, LocalDate sessionDate, String notes) {}
    public record SetEntry(Long id, Long sessionId, String exerciseName, String muscleGroup, int setNumber, BigDecimal weight, int reps, String comment) {}
}
