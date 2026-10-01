package com.example.proyectolp.model;
import com.fasterxml.jackson.annotation.JsonProperty;
public class Habit {
    private long id; @JsonProperty("created_at") private String createdAt; private String name; private String description; private boolean active=true;
    public Habit() {} public long getId(){return id;} public void setId(long v){id=v;} public String getCreatedAt(){return createdAt;} public void setCreatedAt(String v){createdAt=v;}
    public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
