package com.example.proyectolp.model;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserStats { @JsonProperty(value="user_id", access=JsonProperty.Access.WRITE_ONLY) public String user_id; public int current_streak,best_streak; public LocalDate last_activity_date,last_penalty_for; public UserStats(){} }
