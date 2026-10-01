package com.example.proyectolp.model;
import com.fasterxml.jackson.annotation.JsonProperty;
public class ConnectionTest {
    private long id; @JsonProperty("created_at") private String createdAt; private String message;
    public ConnectionTest() {} public long getId(){return id;} public void setId(long v){id=v;} public String getCreatedAt(){return createdAt;} public void setCreatedAt(String v){createdAt=v;}
    public String getMessage(){return message;} public void setMessage(String v){message=v;}
}
