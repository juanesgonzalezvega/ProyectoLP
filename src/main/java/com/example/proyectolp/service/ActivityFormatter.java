package com.example.proyectolp.service;

import com.example.proyectolp.model.ActivityEntry;
import com.example.proyectolp.model.enums.ActivityCategory;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ActivityFormatter {
    private static final Locale LOCALE = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("d MMM uuuu, h:mm a", LOCALE);
    private final Clock clock;
    private final ZoneId zone;

    public ActivityFormatter(Clock clock) {
        this(clock, ActivityQueryBuilder.BUSINESS_ZONE);
    }

    public ActivityFormatter(Clock clock, ZoneId zone) {
        this.clock = clock;
        this.zone = zone;
    }

    public String formatDate(Instant instant) {
        if (instant == null) return "Fecha no disponible";
        return DATE_TIME.format(instant.atZone(zone));
    }

    public String relativeTime(Instant instant) {
        if (instant == null) return "Fecha no disponible";
        Instant now = clock.instant();
        Duration elapsed = Duration.between(instant, now);
        if (elapsed.isNegative() || elapsed.toMinutes() < 1) return "Ahora";
        if (elapsed.toHours() < 1) return "Hace " + elapsed.toMinutes() + " min";
        LocalDate eventDay = instant.atZone(zone).toLocalDate();
        LocalDate today = now.atZone(zone).toLocalDate();
        if (eventDay.equals(today.minusDays(1))) return "Ayer, " + DATE_TIME.format(instant.atZone(zone));
        if (elapsed.toHours() < 24) return "Hace " + elapsed.toHours() + " h";
        return formatDate(instant);
    }

    public String categoryLabel(String key) {
        if (key == null) return "Sin categoría";
        return ActivityCategory.fromKey(key).map(ActivityCategory::label).orElse("Sin categoría");
    }

    public List<String> changeDetails(JsonNode metadata) {
        List<String> details = new ArrayList<>();
        if (metadata == null || !metadata.isObject()) return details;
        JsonNode changes = metadata.path("cambios").isObject() ? metadata.path("cambios") : metadata;
        changes.fields().forEachRemaining(field -> {
            JsonNode value = field.getValue();
            if (value != null && value.isObject() && value.has("antes") && value.has("despues")) {
                details.add(truncate(field.getKey()) + ": " + display(value.get("antes"))
                        + " → " + display(value.get("despues")));
            }
        });
        if (changes == metadata && metadata.has("antes") && metadata.has("despues")) {
            details.add("valor: " + display(metadata.get("antes")) + " → " + display(metadata.get("despues")));
        }
        return List.copyOf(details);
    }

    public String detailText(ActivityEntry entry) {
        if (entry == null || entry.metadata == null || !entry.metadata.isObject()) return "";
        return String.join(System.lineSeparator(), changeDetails(entry.metadata));
    }

    public String truncate(String value) {
        if (value == null) return "";
        return value.length() <= 200 ? value : value.substring(0, 199) + "…";
    }

    private String display(JsonNode value) {
        if (value == null || value.isNull()) return "—";
        if (value.isTextual()) return truncate(value.asText());
        return truncate(value.toString());
    }
}
