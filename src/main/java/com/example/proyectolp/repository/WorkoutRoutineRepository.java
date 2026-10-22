package com.example.proyectolp.repository;

import com.example.proyectolp.model.TrainingModels.*;
import com.example.proyectolp.service.SupabaseService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.*;

public class WorkoutRoutineRepository {
    private final SupabaseService db;
    public WorkoutRoutineRepository(SupabaseService db) { this.db = db; }
    public List<Routine> list() throws Exception {
        List<Row> rows = db.getList("workout_routines?select=*&order=name", Row.class);
        ExerciseRepository er = new ExerciseRepository(db);
        List<Routine> result = new ArrayList<>();
        for (Row r : rows) result.add(new Routine(r.id,r.name,r.description,er.listByRoutine(r.id)));
        return result;
    }
    public long create(String name,String description) throws Exception { return db.postList("workout_routines",Map.of("name",name,"description",description==null?"":description),"return=representation",Row.class).get(0).id; }
    public void update(long id,String name,String description) throws Exception { db.patch("workout_routines?id=eq."+id,Map.of("name",name,"description",description==null?"":description),"return=minimal"); }
    public void delete(long id) throws Exception { db.delete("workout_routines?id=eq."+id); }
    @JsonIgnoreProperties(ignoreUnknown=true) public static class Row { public long id; public String name,description; public Row(){} }
}
