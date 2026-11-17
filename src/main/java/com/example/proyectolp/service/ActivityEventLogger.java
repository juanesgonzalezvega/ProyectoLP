package com.example.proyectolp.service;

import com.example.proyectolp.repository.ActivityRepository;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

public final class ActivityEventLogger {
    public static final Set<String> ALLOWED_ACTIONS = Set.of(
            "inicio_sesion", "cierre_sesion", "contrasena_actualizada", "entrenamiento_finalizado"
    );

    @FunctionalInterface
    public interface ClientEventWriter {
        void write(String action, Map<String, ?> metadata) throws Exception;
    }

    private final ClientEventWriter writer;
    private final Executor executor;

    public ActivityEventLogger(ClientEventWriter writer, Executor executor) {
        this.writer = writer;
        this.executor = executor;
    }

    public ActivityEventLogger(SupabaseService db, Executor executor) {
        this(new ActivityRepository(db)::logClientEvent, executor);
    }

    public boolean logBestEffort(String action) {
        return logBestEffort(action, Map.of());
    }

    public boolean logBestEffort(String action, Map<String, ?> metadata) {
        if (!ALLOWED_ACTIONS.contains(action)) return false;
        try {
            executor.execute(() -> logSynchronously(action, metadata));
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public boolean logSynchronously(String action) {
        return logSynchronously(action, Map.of());
    }

    public boolean logSynchronously(String action, Map<String, ?> metadata) {
        if (!ALLOWED_ACTIONS.contains(action)) return false;
        try {
            writer.write(action, metadata == null ? Map.of() : metadata);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public void logCloseBefore(Runnable revoke) {
        logSynchronously("cierre_sesion");
        revoke.run();
    }
}
