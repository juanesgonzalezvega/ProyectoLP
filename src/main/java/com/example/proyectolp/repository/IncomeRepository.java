package com.example.proyectolp.repository;

import com.example.proyectolp.model.FinancialMovement;
import com.example.proyectolp.service.SupabaseService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.*;

public class IncomeRepository {
    private final SupabaseService supabase;
    public IncomeRepository(SupabaseService supabase){this.supabase=supabase;}
    public List<FinancialMovement> listar() throws Exception { return map(supabase.getList("income?select=*&order=date.desc", Row.class)); }
    public void crear(FinancialMovement m) throws Exception { supabase.post("income", fields(m), "return=minimal"); }
    public void actualizar(FinancialMovement m) throws Exception { supabase.patch("income?id=eq."+m.getId(), fields(m), "return=minimal"); }
    public void eliminar(long id) throws Exception { supabase.delete("income?id=eq."+id); }
    private Map<String,Object> fields(FinancialMovement m){return Map.of("amount",m.getAmount(),"category",m.getCategory(),"description",m.getDescription()==null?"":m.getDescription(),"date",m.getDate());}
    private List<FinancialMovement> map(List<Row> rows){return rows.stream().map(r->new FinancialMovement(r.id,r.amount,r.category,r.description,r.date,FinancialMovement.Type.INGRESO)).toList();}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row { public long id; public java.math.BigDecimal amount; public String category,description; public java.time.LocalDate date; public Row(){} }
}
