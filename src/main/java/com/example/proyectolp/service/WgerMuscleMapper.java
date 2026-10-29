package com.example.proyectolp.service;

import com.example.proyectolp.controller.RoutineFormController;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** Convierte la taxonomía de músculos y categorías de wger a los ocho grupos permitidos. */
public final class WgerMuscleMapper {
    private WgerMuscleMapper() { }

    public static String map(String muscle, String category) {
        String m = normalize(muscle);
        String c = normalize(category);
        String group = switch (m) {
            case "pectoralis major", "chest", "pecho" -> "pecho";
            case "latissimus dorsi", "trapezius", "lower back", "back", "espalda" -> "espalda";
            case "anterior deltoid", "posterior deltoid", "shoulders", "deltoid", "hombros" -> "hombros";
            case "biceps brachii", "brachialis", "biceps", "biceps braquial" -> "bíceps";
            case "triceps brachii", "triceps" -> "tríceps";
            case "quadriceps femoris", "hamstrings", "biceps femoris", "gluteus maximus", "gastrocnemius", "soleus", "quads", "glutes", "calves", "legs" -> "piernas";
            case "rectus abdominis", "obliquus externus", "abs", "abdominals", "obliques" -> "core";
            default -> "";
        };
        if (group.isEmpty()) group = switch (c) {
            case "chest" -> "pecho";
            case "back" -> "espalda";
            case "shoulders" -> "hombros";
            case "arms" -> "otro";
            case "legs", "calves" -> "piernas";
            case "abs" -> "core";
            default -> "otro";
        };
        return RoutineFormController.MUSCLES.contains(group) ? group : "otro";
    }

    public static String map(List<String> primaryMuscles, String category) {
        if (primaryMuscles != null) for (String muscle : primaryMuscles) {
            String mapped = map(muscle, null);
            if (!mapped.equals("otro")) return mapped;
        }
        // Evita la ambigüedad entre map(String, String) y map(List<String>, String).
        return map((String) null, category);
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
