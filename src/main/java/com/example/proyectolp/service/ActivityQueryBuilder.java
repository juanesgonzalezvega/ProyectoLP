package com.example.proyectolp.service;

import com.example.proyectolp.model.enums.ActivityCategory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class ActivityQueryBuilder {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");

    private ActivityQueryBuilder() {
    }

    public static String build(ActivityCategory category, LocalDate from, LocalDate through,
                               String search, int limit, int offset) {
        return build(category, from, through, search, limit, offset, BUSINESS_ZONE);
    }

    public static String build(ActivityCategory category, LocalDate from, LocalDate through,
                               String search, int limit, int offset, ZoneId zone) {
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("El límite debe estar entre 1 y 1000.");
        if (offset < 0) throw new IllegalArgumentException("El desplazamiento no puede ser negativo.");
        if (from != null && through != null && through.isBefore(from)) {
            throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la fecha inicial.");
        }

        List<String> filters = new ArrayList<>();
        if (category != null && category != ActivityCategory.ALL) {
            filters.add("category=eq." + encode(category.key()));
        }
        if (from != null) {
            filters.add("occurred_at=gte." + encode(from.atStartOfDay(zone).toInstant().toString()));
        }
        if (through != null) {
            filters.add("occurred_at=lt." + encode(through.plusDays(1).atStartOfDay(zone).toInstant().toString()));
        }
        if (search != null && !search.isBlank()) {
            String escaped = search.trim()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_")
                    .replace("*", "\\*");
            filters.add("summary=ilike.*" + encode(escaped) + "*");
        }
        filters.add("order=occurred_at.desc,id.desc");
        filters.add("limit=" + limit);
        filters.add("offset=" + offset);
        return "activity_log?select=id,user_id,occurred_at,category,action,entity_type,entity_id,summary,metadata,source&"
                + String.join("&", filters);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
