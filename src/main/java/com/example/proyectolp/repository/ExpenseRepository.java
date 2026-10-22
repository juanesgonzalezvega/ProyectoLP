package com.example.proyectolp.repository;

import com.example.proyectolp.model.FinancialMovement;
import com.example.proyectolp.service.SupabaseService;
import java.util.*;

public class ExpenseRepository {
    private final SupabaseService supabase;
    public ExpenseRepository(SupabaseService supabase){this.supabase=supabase;}
    public List<FinancialMovement> listar() throws Exception { return supabase.getList("expenses?select=*&order=date.desc", IncomeRepository.Row.class).stream().map(r->new FinancialMovement(r.id,r.amount,r.category,r.description,r.date,FinancialMovement.Type.GASTO)).toList(); }
    public void crear(FinancialMovement m) throws Exception { supabase.post("expenses", fields(m), "return=minimal"); com.example.proyectolp.service.GamificationRuntime.service().financialMovement(m.getId()); }
    public void actualizar(FinancialMovement m) throws Exception { supabase.patch("expenses?id=eq."+m.getId(), fields(m), "return=minimal"); }
    public void eliminar(long id) throws Exception { supabase.delete("expenses?id=eq."+id); }
    private Map<String,Object> fields(FinancialMovement m){Map<String,Object> values=new LinkedHashMap<>();values.put("amount",m.getAmount());values.put("category",m.getCategory());values.put("description",m.getDescription());values.put("date",m.getDate());return values;}
}
