package com.example.proyectolp.model;

import java.time.LocalDate;

public class HabitHistoryDay {
    private final LocalDate date;
    private final boolean completed;
    public HabitHistoryDay(LocalDate date, boolean completed) { this.date = date; this.completed = completed; }
    public LocalDate getDate() { return date; }
    public String getStatus() { return completed ? "Cumplido" : "No cumplido"; }
}
