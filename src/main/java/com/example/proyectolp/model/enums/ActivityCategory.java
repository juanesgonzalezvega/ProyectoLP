package com.example.proyectolp.model.enums;

import java.util.Arrays;
import java.util.Optional;

public enum ActivityCategory {
    ALL("", "Todas"),
    TASK("tarea", "Tareas"),
    PROJECT("proyecto", "Proyectos"),
    HABIT("habito", "Hábitos"),
    FINANCE("finanzas", "Finanzas"),
    TRAINING("entrenamiento", "Entrenamiento"),
    CREDITS("creditos", "Créditos"),
    REDEMPTION("canje", "Canjes"),
    BADGE("insignia", "Insignias"),
    MISSION("mision", "Misiones"),
    ATTACHMENT("adjunto", "Adjuntos"),
    ACCOUNT("cuenta", "Cuenta");

    private final String key;
    private final String label;

    ActivityCategory(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String key() { return key; }
    public String label() { return label; }
    @Override public String toString() { return label; }

    public static Optional<ActivityCategory> fromKey(String key) {
        return Arrays.stream(values()).filter(category -> category.key.equals(key)).findFirst();
    }
}
