package com.example.proyectolp.repository;

import com.example.proyectolp.model.Habit;
import com.example.proyectolp.model.HabitLog;
import com.example.proyectolp.service.SupabaseService;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HabitRepository {
    private final SupabaseService supabase;
    public HabitRepository(SupabaseService supabase) { this.supabase = supabase; }

    public List<Habit> listar() throws Exception {
        return supabase.getList("habits?select=id,created_at,name,description,active&order=name.asc", Habit.class);
    }
    public List<Habit> listarActivos() throws Exception {
        return supabase.getList("habits?select=id,created_at,name,description,active&active=eq.true&order=name.asc", Habit.class);
    }
    public List<Habit> findActive() throws Exception { return listarActivos(); }
    public List<HabitLog> listarLogsPorHabitoEnRango(long habitId, LocalDate from, LocalDate to) throws Exception {
        return supabase.getList("habit_logs?select=id,created_at,habit_id,log_date,completed&habit_id=eq." + habitId
                + "&log_date=gte." + from + "&log_date=lte." + to + "&order=log_date.asc", HabitLog.class);
    }
    public List<HabitLog> listarTodosLogs() throws Exception {
        List<HabitLog> result = new java.util.ArrayList<>();
        int pageSize = 1000, offset = 0;
        while (true) {
            List<HabitLog> page = supabase.getList("habit_logs?select=id,created_at,habit_id,log_date,completed&order=log_date.asc,id.asc&limit=" + pageSize + "&offset=" + offset, HabitLog.class);
            result.addAll(page);
            if (page.size() < pageSize) return result;
            offset += pageSize;
        }
    }
    public List<HabitLog> findLogs(LocalDate date) throws Exception {
        return supabase.getList("habit_logs?select=habit_id,log_date,completed&log_date=eq." + date + "&completed=eq.true", HabitLog.class);
    }

    public Habit crear(Habit habit) throws Exception {
        List<Habit> rows = supabase.postList("habits", habitFields(habit), "return=representation", Habit.class);
        if (rows.isEmpty()) throw new IllegalStateException("Supabase no devolvió el hábito creado.");
        return rows.get(0);
    }
    public void actualizar(Habit habit) throws Exception {
        supabase.patch("habits?id=eq." + habit.getId(), habitFields(habit), "return=minimal");
    }
    public void actualizarActivo(long habitId, boolean active) throws Exception {
        supabase.patch("habits?id=eq." + habitId, Map.of("active", active), "return=minimal");
    }

    /** Inserta o actualiza el cumplimiento usando la clave única del esquema. */
    public void upsertLog(long habitId, LocalDate date, boolean completed) throws Exception {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("habit_id", habitId); values.put("log_date", date); values.put("completed", completed);
        supabase.post("habit_logs?on_conflict=habit_id,log_date", values, "resolution=merge-duplicates,return=minimal");
        com.example.proyectolp.service.GamificationRuntime.service().habitCompleted(habitId,date,completed);
    }

    private Map<String, Object> habitFields(Habit habit) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", habit.getName()); values.put("description", habit.getDescription()); values.put("active", habit.isActive());
        return values;
    }
}
