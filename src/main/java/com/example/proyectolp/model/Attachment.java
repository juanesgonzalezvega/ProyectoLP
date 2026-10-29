package com.example.proyectolp.model;

import com.example.proyectolp.model.enums.AttachmentKind;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Attachment {
    private Long id;
    @JsonProperty("task_id") private Long taskId;
    @JsonProperty("project_id") private Long projectId;
    @JsonProperty("session_id") private Long sessionId;
    @JsonProperty("file_name") private String fileName;
    @JsonProperty("mime_type") private String mimeType;
    private AttachmentKind kind;
    @JsonProperty("size_bytes") private long sizeBytes;
    @JsonProperty("storage_path") private String storagePath;
    @JsonProperty("created_at") private Instant createdAt;
    @JsonIgnore private String userId;

    public Attachment() { }
    public Attachment(Long taskId,Long projectId,Long sessionId,String fileName,String mimeType,AttachmentKind kind,long sizeBytes,String storagePath){this.taskId=taskId;this.projectId=projectId;this.sessionId=sessionId;this.fileName=fileName;this.mimeType=mimeType;this.kind=kind;this.sizeBytes=sizeBytes;this.storagePath=storagePath;}
    public Long getId(){return id;} public void setId(Long value){id=value;}
    public Long getTaskId(){return taskId;} public void setTaskId(Long value){taskId=value;}
    public Long getProjectId(){return projectId;} public void setProjectId(Long value){projectId=value;}
    public Long getSessionId(){return sessionId;} public void setSessionId(Long value){sessionId=value;}
    public String getFileName(){return fileName;} public void setFileName(String value){fileName=value;}
    public String getMimeType(){return mimeType;} public void setMimeType(String value){mimeType=value;}
    public AttachmentKind getKind(){return kind;} public void setKind(AttachmentKind value){kind=value;}
    public long getSizeBytes(){return sizeBytes;} public void setSizeBytes(long value){sizeBytes=value;}
    public String getStoragePath(){return storagePath;} public void setStoragePath(String value){storagePath=value;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant value){createdAt=value;}
    @JsonIgnore public String getUserId(){return userId;} @JsonIgnore public void setUserId(String value){userId=value;}
}
