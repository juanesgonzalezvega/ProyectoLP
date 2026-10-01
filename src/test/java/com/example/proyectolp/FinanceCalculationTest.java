package com.example.proyectolp;

import com.example.proyectolp.model.FinancialGoal;
import com.example.proyectolp.model.FinancialMovement;
import com.example.proyectolp.service.FinanceCalculationService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FinanceCalculationTest {
    private final FinanceCalculationService service = new FinanceCalculationService();
    @Test void balanceIngresosMenosGastos(){var b=service.balance(List.of(m("1000",FinancialMovement.Type.INGRESO,"Salario"),m("250",FinancialMovement.Type.GASTO,"Mercado"),m("50",FinancialMovement.Type.GASTO,"Transporte")));assertEquals(new BigDecimal("1000"),b.income());assertEquals(new BigDecimal("300"),b.expenses());assertEquals(new BigDecimal("700"),b.available());}
    @Test void avanceDeMetaNuncaSuperaCien(){assertEquals(new BigDecimal("100"),service.progreso(goal("100","125")));assertEquals(BigDecimal.ZERO,service.restante(goal("100","125")));}
    @Test void calculaPorcentajeParcialDeMeta(){assertEquals(new BigDecimal("37.50"),service.progreso(goal("800","300")));}
    @Test void agrupaGastosPorCategoria(){var totals=service.gastosPorCategoria(List.of(m("20",FinancialMovement.Type.GASTO,"Comida"),m("15",FinancialMovement.Type.GASTO,"Comida"),m("10",FinancialMovement.Type.GASTO,"Transporte"),m("100",FinancialMovement.Type.INGRESO,"Salario")));assertEquals(new BigDecimal("35"),totals.get("Comida"));assertEquals(new BigDecimal("10"),totals.get("Transporte"));}
    private static FinancialMovement m(String amount,FinancialMovement.Type type,String category){return new FinancialMovement(1,new BigDecimal(amount),category,null,LocalDate.of(2026,9,1),type);}
    private static FinancialGoal goal(String target,String saved){FinancialGoal g=new FinancialGoal();g.setTargetAmount(new BigDecimal(target));g.setSavedAmount(new BigDecimal(saved));return g;}
}
