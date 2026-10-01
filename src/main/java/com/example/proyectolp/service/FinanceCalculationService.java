package com.example.proyectolp.service;

import com.example.proyectolp.model.FinancialMovement;
import com.example.proyectolp.model.FinancialGoal;
import java.math.*;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class FinanceCalculationService {
    public record Balance(BigDecimal income, BigDecimal expenses, BigDecimal available) {}
    public Balance balance(List<FinancialMovement> movements){
        BigDecimal income=total(movements,FinancialMovement.Type.INGRESO), expenses=total(movements,FinancialMovement.Type.GASTO);
        return new Balance(income,expenses,income.subtract(expenses));
    }
    public Balance balanceMes(List<FinancialMovement> movements, YearMonth month){return balance(movements.stream().filter(m->YearMonth.from(m.getDate()).equals(month)).toList());}
    public Map<String,BigDecimal> gastosPorCategoria(List<FinancialMovement> movements){return movements.stream().filter(m->m.getType()==FinancialMovement.Type.GASTO).collect(Collectors.groupingBy(m->m.getCategory()==null?"Sin categoría":m.getCategory(),TreeMap::new,Collectors.reducing(BigDecimal.ZERO,FinancialMovement::getAmount,BigDecimal::add)));}
    public Map<YearMonth,Balance> evolucionMensual(List<FinancialMovement> movements){return movements.stream().collect(Collectors.groupingBy(m->YearMonth.from(m.getDate()),TreeMap::new,Collectors.collectingAndThen(Collectors.toList(),this::balance)));}
    public BigDecimal progreso(FinancialGoal goal){if(goal.getTargetAmount()==null||goal.getTargetAmount().signum()<=0)return BigDecimal.ZERO;return goal.getSavedAmount().multiply(BigDecimal.valueOf(100)).divide(goal.getTargetAmount(),2,RoundingMode.HALF_UP).min(BigDecimal.valueOf(100)).max(BigDecimal.ZERO);}
    public BigDecimal restante(FinancialGoal goal){return goal.getTargetAmount().subtract(goal.getSavedAmount()).max(BigDecimal.ZERO);}
    private BigDecimal total(List<FinancialMovement> ms,FinancialMovement.Type type){return ms.stream().filter(m->m.getType()==type).map(FinancialMovement::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);}
}
