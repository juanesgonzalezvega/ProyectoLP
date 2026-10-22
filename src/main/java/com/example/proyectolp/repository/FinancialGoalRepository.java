package com.example.proyectolp.repository;

import com.example.proyectolp.model.FinancialGoal;
import com.example.proyectolp.service.SupabaseService;
import java.math.BigDecimal;
import java.util.*;

public class FinancialGoalRepository {
    private final SupabaseService supabase;
    public FinancialGoalRepository(SupabaseService supabase){this.supabase=supabase;}
    public List<FinancialGoal> listar() throws Exception{return supabase.getList("financial_goals?select=*&order=created_at.desc",FinancialGoal.class);}
    public void crear(FinancialGoal goal)throws Exception{supabase.post("financial_goals",fields(goal),"return=minimal");}
    public void actualizar(FinancialGoal goal)throws Exception{supabase.patch("financial_goals?id=eq."+goal.getId(),fields(goal),"return=minimal");}
    public void eliminar(long id)throws Exception{supabase.delete("financial_goals?id=eq."+id);}
    public void aportar(FinancialGoal goal, BigDecimal aporte)throws Exception{BigDecimal old=goal.getSavedAmount(),next=old.add(aporte);Map<String,Object> value=Map.of("saved_amount",next);supabase.patch("financial_goals?id=eq."+goal.getId(),value,"return=minimal");com.example.proyectolp.service.GamificationRuntime.service().goalContribution(goal.getId(),goal.getTargetAmount(),old,next);goal.setSavedAmount(next);}
    private Map<String,Object> fields(FinancialGoal g){return Map.of("name",g.getName(),"target_amount",g.getTargetAmount(),"saved_amount",g.getSavedAmount());}
}
