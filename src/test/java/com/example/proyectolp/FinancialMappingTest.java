package com.example.proyectolp;

import com.example.proyectolp.model.FinancialGoal;
import com.example.proyectolp.repository.IncomeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancialMappingTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @Test void incomeAceptaLaColumnaAutomaticaCreatedAt() throws Exception {
        IncomeRepository.Row row = mapper.readValue("{\"id\":4,\"amount\":1200,\"category\":\"Salario\",\"date\":\"2026-09-01\",\"created_at\":\"2026-09-01T10:15:30Z\"}", IncomeRepository.Row.class);
        assertEquals(4, row.id);
        assertEquals("1200", row.amount.toPlainString());
    }
    @Test void financialGoalAceptaLaColumnaAutomaticaCreatedAt() throws Exception {
        FinancialGoal goal = mapper.readValue("{\"id\":2,\"name\":\"Ahorro\",\"target_amount\":1000,\"saved_amount\":250,\"created_at\":\"2026-09-01T10:15:30Z\"}", FinancialGoal.class);
        assertEquals("Ahorro", goal.getName());
        assertEquals("250", goal.getSavedAmount().toPlainString());
    }
}
