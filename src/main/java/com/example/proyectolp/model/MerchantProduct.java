package com.example.proyectolp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MerchantProduct {
    public long id;
    @JsonProperty("merchant_id") public String merchantId;
    public String name;
    public String description;
    public String kind;
    @JsonProperty("credit_cost") public int creditCost;
    @JsonProperty("sort_order") public int sortOrder;
    public boolean active;
    public MerchantProduct() {}
}
