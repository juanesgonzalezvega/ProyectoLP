package com.example.proyectolp.repository;

import com.example.proyectolp.model.Project;
import com.example.proyectolp.model.Stage;
import com.example.proyectolp.service.SupabaseService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProjectRepository {
    private final SupabaseService supabase;
    private final StageRepository stageRepository;

    public ProjectRepository(SupabaseService supabase) {
        this.supabase = supabase;
        this.stageRepository = new StageRepository(supabase);
    }

    public List<Project> findActive() throws Exception {
        return listar().stream().filter(p -> p.getStatus() == null || !"completada".equals(p.getStatus())).toList();
    }

    public List<Project> listar() throws Exception {
        return supabase.getList("projects?select=id,created_at,name,main_goal,status,deadline&order=name.asc", Project.class);
    }

    public List<Project> listarConEtapas() throws Exception {
        List<Project> projects = listar();
        if (projects.isEmpty()) return projects;
        List<Long> ids = projects.stream().map(Project::getId).toList();
        List<Stage> stages = stageRepository.listarPorProyectos(ids);
        Map<Long, List<Stage>> byProject = stages.stream().collect(java.util.stream.Collectors.groupingBy(Stage::getProjectId));
        projects.forEach(project -> project.setStages(byProject.getOrDefault(project.getId(), List.of())));
        return projects;
    }

    public Project crear(Project project) throws Exception {
        List<Project> rows = supabase.postList("projects", fields(project), "return=representation", Project.class);
        if (rows.isEmpty()) throw new IllegalStateException("Supabase no devolvió el proyecto creado.");
        return rows.get(0);
    }

    public void actualizar(Project project) throws Exception {
        supabase.patch("projects?id=eq." + project.getId(), fields(project), "return=minimal");
    }

    public void eliminar(long id) throws Exception { supabase.delete("projects?id=eq." + id); }

    public List<Stage> findStages() throws Exception { return stageRepository.listar(); }

    private Map<String, Object> fields(Project project) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", project.getName());
        values.put("main_goal", project.getMainGoal());
        values.put("deadline", project.getDeadline());
        values.put("status", project.getStatus() == null ? "en_progreso" : project.getStatus());
        return values;
    }
}
