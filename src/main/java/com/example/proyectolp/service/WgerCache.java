package com.example.proyectolp.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Caché de tablas de referencia; nunca almacena búsquedas. */
public final class WgerCache {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Path file;
    private final Clock clock;
    public WgerCache() { this(Path.of(System.getProperty("user.home"), ".proyectolp", "wger-cache.json"), Clock.systemUTC()); }
    public WgerCache(Path file, Clock clock) { this.file = file; this.clock = clock; }
    public Tables read() {
        try {
            Entry entry = JSON.readValue(file.toFile(), Entry.class);
            if (entry != null && entry.savedAt != null && Duration.between(entry.savedAt, clock.instant()).compareTo(Duration.ofDays(30)) < 0)
                return new Tables(entry.muscles, entry.categories, entry.language);
        } catch (Exception ignored) { }
        return null;
    }
    public void write(Tables tables) throws IOException {
        Files.createDirectories(file.getParent());
        Entry entry = new Entry(); entry.savedAt = clock.instant(); entry.muscles = tables.muscles();
        entry.categories = tables.categories(); entry.language = tables.language();
        JSON.writeValue(file.toFile(), entry);
    }
    public record Tables(List<String> muscles, List<String> categories, String language) { }
    @JsonIgnoreProperties(ignoreUnknown = true) public static final class Entry {
        public Instant savedAt; public List<String> muscles; public List<String> categories; public String language;
    }
}
