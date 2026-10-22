package com.example.proyectolp.repository;

import com.example.proyectolp.model.TrainingModels.Exercise;
import com.example.proyectolp.service.SupabaseService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.*;

public class ExerciseRepository {
    private final SupabaseService db;
    public ExerciseRepository(SupabaseService db) { this.db=db; }
    public List<Exercise> listByRoutine(long id) throws Exception { return db.getList("exercises?select=*&routine_id=eq."+id+"&order=position",Row.class).stream().map(r->new Exercise(r.id,r.routine_id,r.name,r.muscle_group,r.position)).toList(); }
    public void replaceForRoutine(long routineId,List<Exercise> exercises) throws Exception { db.delete("exercises?routine_id=eq."+routineId); for(int i=0;i<exercises.size();i++){ Exercise e=exercises.get(i); db.post("exercises",Map.of("routine_id",routineId,"name",e.name(),"muscle_group",e.muscleGroup(),"position",i),"return=minimal"); } }
    @JsonIgnoreProperties(ignoreUnknown=true) public static class Row { public long id,routine_id; public String name,muscle_group; public int position; public Row(){} }
}
