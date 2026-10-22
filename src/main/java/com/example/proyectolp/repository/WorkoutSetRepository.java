package com.example.proyectolp.repository;

import com.example.proyectolp.model.TrainingModels.SetEntry;
import com.example.proyectolp.service.SupabaseService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.*;

public class WorkoutSetRepository {
    private final SupabaseService db;
    public WorkoutSetRepository(SupabaseService db){this.db=db;}
    public List<SetEntry> list() throws Exception { return db.getList("workout_sets?select=*&order=created_at",Row.class).stream().map(r->new SetEntry(r.id,r.session_id,r.exercise_name,r.muscle_group,r.set_number,r.weight,r.reps,r.comment)).toList(); }
    public void create(SetEntry s) throws Exception { Map<String,Object> m=new LinkedHashMap<>();m.put("session_id",s.sessionId());m.put("exercise_name",s.exerciseName());m.put("muscle_group",s.muscleGroup());m.put("set_number",s.setNumber());m.put("weight",s.weight());m.put("reps",s.reps());m.put("comment",s.comment()==null?"":s.comment());db.post("workout_sets",m,"return=minimal"); }
    @JsonIgnoreProperties(ignoreUnknown=true) public static class Row { public long id,session_id;public String exercise_name,muscle_group,comment;public int set_number,reps;public BigDecimal weight;public Row(){} }
}
