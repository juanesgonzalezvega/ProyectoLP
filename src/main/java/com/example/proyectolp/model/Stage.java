package com.example.proyectolp.model;
import com.fasterxml.jackson.annotation.JsonProperty;
public class Stage {
    private long id; @JsonProperty("created_at") private String createdAt;
    @JsonProperty("project_id") private long projectId; private String name; private int position; private String status;
    public Stage() {}
    public long getId(){return id;} public void setId(long v){id=v;} public String getCreatedAt(){return createdAt;} public void setCreatedAt(String v){createdAt=v;}
    public long getProjectId(){return projectId;} public void setProjectId(long v){projectId=v;} public String getName(){return name;} public void setName(String v){name=v;}
    public int getPosition(){return position;} public void setPosition(int v){position=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
    @Override public String toString(){return position + ". " + name + " [" + status + "]";}
}
