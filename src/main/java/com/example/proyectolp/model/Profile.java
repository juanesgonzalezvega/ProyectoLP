package com.example.proyectolp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Profile {
    @JsonProperty(value="user_id", access=JsonProperty.Access.WRITE_ONLY) public String userId;
    @JsonProperty("display_name") public String displayName;
    public Profile() {}
    public Profile(String userId, String displayName) { this.userId = userId; this.displayName = displayName; }
}
