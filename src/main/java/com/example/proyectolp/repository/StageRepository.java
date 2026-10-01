package com.example.proyectolp.repository;

import com.example.proyectolp.model.Stage;
import com.example.proyectolp.service.SupabaseService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StageRepository {
    private final SupabaseService supabase;
    public StageRepository(SupabaseService supabase) { this.supabase = supabase; }

    public List<Stage> listar() throws Exception {
        return supabase.getList("stages?select=id,created_at,project_id,name,position,status&order=project_id.asc,position.asc", Stage.class);
    }
    public List<Stage> listarPorProyecto(long projectId) throws Exception {
        return supabase.getList("stages?select=id,created_at,project_id,name,position,status&project_id=eq." + projectId + "&order=position.asc", Stage.class);
    }
    public List<Stage> listarPorProyectos(List<Long> projectIds) throws Exception {
        if (projectIds.isEmpty()) return List.of();
        String ids = String.join(",", projectIds.stream().map(String::valueOf).toList());
        return supabase.getList("stages?select=id,created_at,project_id,name,position,status&project_id=in.(" + ids + ")&order=position.asc", Stage.class);
    }
    public Stage crear(Stage stage) throws Exception {
        List<Stage> rows = supabase.postList("stages", fields(stage), "return=representation", Stage.class);
        if (rows.isEmpty()) throw new IllegalStateException("Supabase no devolvió la etapa creada.");
        return rows.get(0);
    }
    public void actualizar(Stage stage) throws Exception { supabase.patch("stages?id=eq." + stage.getId(), fields(stage), "return=minimal"); }
    public void eliminar(long id) throws Exception { supabase.delete("stages?id=eq." + id); }

    /** Sincroniza altas, bajas, nombres, estados y orden del formulario. */
    public void sincronizar(long projectId, List<Stage> desiredStages) throws Exception {
        List<Stage> existing = listarPorProyecto(projectId);
        List<Long> retainedIds = desiredStages.stream().filter(s -> s.getId() > 0).map(Stage::getId).toList();
        for (Stage old : existing) if (!retainedIds.contains(old.getId())) eliminar(old.getId());
        for (int index = 0; index < desiredStages.size(); index++) {
            Stage stage = desiredStages.get(index);
            stage.setProjectId(projectId); stage.setPosition(index + 1);
            if (stage.getId() > 0) actualizar(stage); else crear(stage);
        }
    }

    private Map<String, Object> fields(Stage stage) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("project_id", stage.getProjectId()); values.put("name", stage.getName());
        values.put("position", stage.getPosition()); values.put("status", stage.getStatus() == null ? "pendiente" : stage.getStatus());
        return values;
    }
}
