package com.example.proyectolp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinancialMovement {
    public enum Type { INGRESO, GASTO }
    private long id;
    private BigDecimal amount = BigDecimal.ZERO;
    private String category;
    private String description;
    private LocalDate date;
    private Type type;
    public FinancialMovement() {}
    public FinancialMovement(long id, BigDecimal amount, String category, String description, LocalDate date, Type type) {
        this.id=id; this.amount=amount; this.category=category; this.description=description; this.date=date; this.type=type;
    }
    public long getId(){return id;} public void setId(long value){id=value;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal value){amount=value;}
    public String getCategory(){return category;} public void setCategory(String value){category=value;}
    public String getDescription(){return description;} public void setDescription(String value){description=value;}
    public LocalDate getDate(){return date;} public void setDate(LocalDate value){date=value;}
    public Type getType(){return type;} public void setType(Type value){type=value;}
}
