package com.example.proyectolp.service;

import com.example.proyectolp.model.ActivityEntry;
import com.example.proyectolp.model.enums.ActivityCategory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ActivityHistoryService {
    public static final int PAGE_SIZE = 50;

    @FunctionalInterface
    public interface Repository {
        List<ActivityEntry> list(Filter filter, int limit, int offset) throws Exception;
    }

    public record Filter(ActivityCategory category, LocalDate from, LocalDate through, String search) {
        public Filter {
            category = category == null ? ActivityCategory.ALL : category;
            search = search == null ? "" : search;
        }
    }

    public record Snapshot(List<ActivityEntry> entries, boolean hasMore, int nextOffset) {
        public Snapshot {
            entries = List.copyOf(entries);
        }
    }

    private final Repository repository;
    private List<ActivityEntry> entries = List.of();
    private Filter filter = new Filter(ActivityCategory.ALL, null, null, "");
    private boolean hasMore;
    private int nextOffset;

    public ActivityHistoryService(Repository repository) {
        this.repository = repository;
    }

    public synchronized Snapshot loadFirst(Filter requested) throws Exception {
        List<ActivityEntry> page = requirePage(repository.list(requested, PAGE_SIZE, 0));
        entries = deduplicate(page);
        filter = requested;
        nextOffset = page.size();
        hasMore = page.size() == PAGE_SIZE;
        return snapshot();
    }

    public synchronized Snapshot loadMore() throws Exception {
        if (!hasMore) return snapshot();
        List<ActivityEntry> page = requirePage(repository.list(filter, PAGE_SIZE, nextOffset));
        Map<Long, ActivityEntry> unique = new LinkedHashMap<>();
        entries.forEach(entry -> unique.put(entry.id, entry));
        page.forEach(entry -> unique.putIfAbsent(entry.id, entry));
        entries = List.copyOf(unique.values());
        nextOffset += page.size();
        hasMore = page.size() == PAGE_SIZE;
        return snapshot();
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(entries, hasMore, nextOffset);
    }

    private static List<ActivityEntry> requirePage(List<ActivityEntry> page) {
        if (page == null) throw new IllegalStateException("El repositorio devolvió una página nula.");
        return page;
    }

    private static List<ActivityEntry> deduplicate(List<ActivityEntry> page) {
        Map<Long, ActivityEntry> unique = new LinkedHashMap<>();
        page.forEach(entry -> unique.putIfAbsent(entry.id, entry));
        return new ArrayList<>(unique.values());
    }
}
