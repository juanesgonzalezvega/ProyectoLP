package com.example.proyectolp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FinancialGoal {
    private long id;
    private String name;
    @JsonProperty("target_amount") private BigDecimal targetAmount = BigDecimal.ZERO;
    @JsonProperty("saved_amount") private BigDecimal savedAmount = BigDecimal.ZERO;
    public FinancialGoal() {}
    public long getId(){return id;} public void setId(long value){id=value;}
    public String getName(){return name;} public void setName(String value){name=value;}
    public BigDecimal getTargetAmount(){return targetAmount;} public void setTargetAmount(BigDecimal value){targetAmount=value;}
    public BigDecimal getSavedAmount(){return savedAmount;} public void setSavedAmount(BigDecimal value){savedAmount=value;}
}
