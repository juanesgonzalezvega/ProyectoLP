package com.example.proyectolp.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class HabitSummary {
    private final Habit habit;
    private final int currentStreak;
    private final int bestStreak;
    private final int compliancePercent;
    private final BooleanProperty completedOnSelectedDate;

    public HabitSummary(Habit habit, int currentStreak, int bestStreak, int compliancePercent, boolean completed) {
        this.habit = habit; this.currentStreak = currentStreak; this.bestStreak = bestStreak;
        this.compliancePercent = compliancePercent; this.completedOnSelectedDate = new SimpleBooleanProperty(completed);
    }
    public Habit getHabit() { return habit; }
    public long getId() { return habit.getId(); }
    public String getName() { return habit.getName(); }
    public int getCurrentStreak() { return currentStreak; }
    public int getBestStreak() { return bestStreak; }
    public String getComplianceText() { return compliancePercent + "%"; }
    public String getActiveText() { return habit.isActive() ? "Activo" : "Inactivo"; }
    public BooleanProperty completedOnSelectedDateProperty() { return completedOnSelectedDate; }
    public boolean isCompletedOnSelectedDate() { return completedOnSelectedDate.get(); }
}
