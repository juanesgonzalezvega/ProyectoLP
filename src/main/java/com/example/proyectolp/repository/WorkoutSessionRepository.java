package com.example.proyectolp.repository;

import com.example.proyectolp.model.TrainingModels.Session;
import com.example.proyectolp.service.SupabaseService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.*;

public class WorkoutSessionRepository {
    private final SupabaseService db;
    public WorkoutSessionRepository(SupabaseService db){this.db=db;}
    public List<Session> list() throws Exception { return db.getList("workout_sessions?select=*&order=session_date.desc",Row.class).stream().map(r->new Session(r.id,r.routine_id,r.session_date,r.notes)).toList(); }
    public long create(Long routineId,LocalDate date,String notes) throws Exception { Map<String,Object> m=new LinkedHashMap<>(); if(routineId!=null)m.put("routine_id",routineId);m.put("session_date",date);m.put("notes",notes==null?"":notes);return db.postList("workout_sessions",m,"return=representation",Row.class).get(0).id; }
    @JsonIgnoreProperties(ignoreUnknown=true) public static class Row { public long id; public Long routine_id; public LocalDate session_date; public String notes; public Row(){} }
}
