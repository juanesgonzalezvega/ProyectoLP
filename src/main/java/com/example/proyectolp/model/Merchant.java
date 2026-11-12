package com.example.proyectolp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Merchant {
    public String id;
    public String name;
    public String category;
    @JsonProperty("website_url") public String websiteUrl;
    @JsonProperty("sort_order") public int sortOrder;
    public boolean active;
    public Merchant() {}
}
