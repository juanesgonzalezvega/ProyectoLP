package com.example.proyectolp.repository;

import com.example.proyectolp.model.TaskItem;
import com.example.proyectolp.service.SupabaseService;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class TaskRepository {
    private final SupabaseService supabase;
    public TaskRepository(SupabaseService supabase) { this.supabase = supabase; }
    public List<TaskItem> listar() throws Exception { return supabase.getList("tasks?select=*&order=created_at.desc", TaskItem.class); }
    public List<TaskItem> listarPorEstado(String status) throws Exception {
        return supabase.getList("tasks?select=*&status=eq." + status + "&order=created_at.desc", TaskItem.class);
    }
    public List<TaskItem> findDashboardTasks() throws Exception { return supabase.getList("tasks?select=id,name,status,due_date,created_at", TaskItem.class); }
    public void crear(TaskItem task) throws Exception { supabase.post("tasks", fields(task), "return=minimal"); }
    public void actualizar(TaskItem task) throws Exception {
        TaskItem before=supabase.getList("tasks?select=id,status,project_id&id=eq."+task.getId(),TaskItem.class).stream().findFirst().orElse(null);
        supabase.patch("tasks?id=eq." + task.getId(), fields(task), "return=minimal");
        if(before!=null&&"completada".equals(task.getStatus())&&!"completada".equals(before.getStatus()))com.example.proyectolp.service.GamificationRuntime.service().taskCompleted(task.getId(),task.getProjectId()!=null);
    }
    public void eliminar(long id) throws Exception {
        new com.example.proyectolp.service.AttachmentService(supabase).deleteAllFor(com.example.proyectolp.model.enums.AttachmentOwnerType.TASK,id);
        supabase.delete("tasks?id=eq." + id);
    }
    private Map<String, Object> fields(TaskItem task) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", task.getName()); values.put("description", task.getDescription());
        values.put("priority", task.getPriority()); values.put("due_date", task.getDueDate());
        values.put("status", task.getStatus()); values.put("progress", task.getProgress());
        values.put("category", task.getCategory()); values.put("project_id", task.getProjectId());
        return values;
    }
}
