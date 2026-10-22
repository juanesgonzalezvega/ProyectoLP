package com.example.proyectolp.repository;

import com.example.proyectolp.model.Session;
import com.example.proyectolp.model.UserStats;
import com.example.proyectolp.service.SessionManager;
import com.example.proyectolp.service.SupabaseService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Acceso a la fila de racha de la cuenta autenticada. */
public class UserStatsRepository {
    private final SupabaseService db;

    public UserStatsRepository(SupabaseService db) { this.db = db; }

    public UserStats get() throws Exception {
        String userId = currentUserId();
        List<UserStats> rows = db.getList(
                "user_stats?select=*&user_id=eq." + userId + "&limit=1",
                UserStats.class);
        if (rows.isEmpty()) throw new IllegalStateException("No existe la fila user_stats de la cuenta. Vuelve a iniciar sesión para inicializarla.");
        return rows.get(0);
    }

    public void save(UserStats stats) throws Exception {
        String userId = currentUserId();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("current_streak", stats.current_streak);
        values.put("best_streak", stats.best_streak);
        values.put("last_activity_date", stats.last_activity_date);
        values.put("last_penalty_for", stats.last_penalty_for);
        db.patch("user_stats?user_id=eq." + userId, values, "return=minimal");
    }

    private String currentUserId() {
        Session session = SessionManager.getInstance().current();
        if (session == null || session.userId() == null || session.userId().isBlank()) {
            throw new IllegalStateException("No hay una sesión activa para actualizar la racha.");
        }
        return session.userId();
    }
}
