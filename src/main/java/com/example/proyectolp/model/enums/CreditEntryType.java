package com.example.proyectolp.model.enums;
public enum CreditEntryType { HABITO("habito"), TAREA("tarea"), TAREA_PROYECTO("tarea_proyecto"), ETAPA_PROYECTO("etapa_proyecto"), META_AHORRO("meta_ahorro"), PENALIZACION("penalizacion"), REDENCION("redencion"); private final String db; CreditEntryType(String db){this.db=db;} public String db(){return db;} }
