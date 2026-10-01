package com.example.proyectolp.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
public class TaskItem {
    private long id; @JsonProperty("created_at") private String createdAt; private String name; private String description; private String priority;
    @JsonProperty("due_date") private LocalDate dueDate; private String status; private int progress; private String category;
    @JsonProperty("project_id") private Long projectId;
    public TaskItem() {}
    public long getId(){return id;} public void setId(long v){id=v;} public String getCreatedAt(){return createdAt;} public void setCreatedAt(String v){createdAt=v;}
    public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getPriority(){return priority;} public void setPriority(String v){priority=v;} public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){dueDate=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;} public int getProgress(){return progress;} public void setProgress(int v){progress=v;}
    public String getCategory(){return category;} public void setCategory(String v){category=v;} public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;}
}
