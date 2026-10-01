package com.example.proyectolp.model.enums;
public enum TaskStatus { PENDIENTE("pendiente"), EN_PROGRESO("en_progreso"), COMPLETADA("completada"); private final String value; TaskStatus(String v){value=v;} public String getValue(){return value;} }
