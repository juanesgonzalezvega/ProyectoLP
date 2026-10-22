package com.example.proyectolp.model;
import java.time.OffsetDateTime;
public record Badge(String code,String name,String description,boolean unlocked,OffsetDateTime unlockedAt) {}
