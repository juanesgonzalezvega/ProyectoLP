package com.example.proyectolp.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
public class HabitLog {
    private long id; @JsonProperty("created_at") private String createdAt; @JsonProperty("habit_id") private long habitId;
    @JsonProperty("log_date") private LocalDate logDate; private boolean completed=true;
    public HabitLog() {} public long getId(){return id;} public void setId(long v){id=v;} public String getCreatedAt(){return createdAt;} public void setCreatedAt(String v){createdAt=v;}
    public long getHabitId(){return habitId;} public void setHabitId(long v){habitId=v;} public LocalDate getLogDate(){return logDate;} public void setLogDate(LocalDate v){logDate=v;}
    public boolean isCompleted(){return completed;} public void setCompleted(boolean v){completed=v;}
}
