package com.example.proyectolp.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
public class Project {
    private long id;
    @JsonProperty("created_at") private String createdAt;
    private String name;
    @JsonProperty("main_goal") private String mainGoal;
    private String status;
    private LocalDate deadline;
    @JsonIgnore private List<Stage> stages = new ArrayList<>();
    public Project() {}
    public long getId(){return id;} public void setId(long v){id=v;}
    public String getCreatedAt(){return createdAt;} public void setCreatedAt(String v){createdAt=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getMainGoal(){return mainGoal;} public void setMainGoal(String v){mainGoal=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public LocalDate getDeadline(){return deadline;} public void setDeadline(LocalDate v){deadline=v;}
    public List<Stage> getStages(){return stages;} public void setStages(List<Stage> v){stages=v == null ? new ArrayList<>() : new ArrayList<>(v);}
}
