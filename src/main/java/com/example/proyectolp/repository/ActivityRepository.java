package com.example.proyectolp.repository;

import com.example.proyectolp.model.ActivityEntry;
import com.example.proyectolp.model.enums.ActivityCategory;
import com.example.proyectolp.service.ActivityHistoryService;
import com.example.proyectolp.service.ActivityQueryBuilder;
import com.example.proyectolp.service.SupabaseService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ActivityRepository implements ActivityHistoryService.Repository {
    private final SupabaseService db;

    public ActivityRepository(SupabaseService db) {
        this.db = db;
    }

    @Override
    public List<ActivityEntry> list(ActivityHistoryService.Filter filter, int limit, int offset) throws Exception {
        return db.getList(ActivityQueryBuilder.build(filter.category(), filter.from(), filter.through(),
                filter.search(), limit, offset), ActivityEntry.class);
    }

    public List<ActivityEntry> latest(int limit) throws Exception {
        if (limit < 1 || limit > 50) throw new IllegalArgumentException("El límite debe estar entre 1 y 50.");
        String path = "activity_log?select=id,user_id,occurred_at,category,action,entity_type,entity_id,summary,metadata,source"
                + "&order=occurred_at.desc,id.desc&limit=" + limit + "&offset=0";
        return db.getList(path, ActivityEntry.class);
    }

    public void logClientEvent(String action, Map<String, ?> metadata) throws Exception {
        db.post("rpc/log_client_event", Map.of(
                "p_action", action,
                "p_metadata", metadata == null ? Map.of() : metadata
        ), "return=minimal");
    }

    public void logClientEvent(String action) throws Exception {
        logClientEvent(action, Map.of());
    }
}
