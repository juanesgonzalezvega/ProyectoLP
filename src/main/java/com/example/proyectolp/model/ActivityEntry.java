package com.example.proyectolp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ActivityEntry {
    public long id;
    @JsonProperty("user_id")
    public String userId;
    @JsonProperty("occurred_at")
    public Instant occurredAt;
    public String category;
    public String action;
    @JsonProperty("entity_type")
    public String entityType;
    @JsonProperty("entity_id")
    public String entityId;
    public String summary;
    public JsonNode metadata;
    public String source;

    public ActivityEntry() {
    }

    public long getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getCategory() { return category; }
    public String getAction() { return action; }
    public String getSummary() { return summary; }
    public JsonNode getMetadata() { return metadata; }
}
