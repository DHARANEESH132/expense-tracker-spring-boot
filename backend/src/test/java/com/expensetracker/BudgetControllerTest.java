package com.expensetracker;

import com.expensetracker.dto.AuthDto.RegisterRequest;
import com.expensetracker.dto.BudgetDto.BudgetRequest;
import com.expensetracker.dto.ExpenseDto.ExpenseRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userAToken;
    private String userBToken;
    private String userABudgetId;

    @BeforeEach
    public void setup() throws Exception {
        // Register User A
        RegisterRequest reqA = new RegisterRequest("User A", "userA@example.com", "Password123!");
        MvcResult resA = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated())
                .andReturn();
        userAToken = objectMapper.readTree(resA.getResponse().getContentAsString()).path("data").path("token").asText();

        // Register User B
        RegisterRequest reqB = new RegisterRequest("User B", "userB@example.com", "Password123!");
        MvcResult resB = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated())
                .andReturn();
        userBToken = objectMapper.readTree(resB.getResponse().getContentAsString()).path("data").path("token").asText();

        // Create a Budget for User A (Food for Sep 2026)
        BudgetRequest bReq = new BudgetRequest();
        bReq.setCategory("Food");
        bReq.setMonth(9);
        bReq.setYear(2026);
        bReq.setAmount(500.0);

        MvcResult bRes = mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bReq)))
                .andExpect(status().isCreated())
                .andReturn();
        userABudgetId = objectMapper.readTree(bRes.getResponse().getContentAsString()).path("data").path("id").asText();
    }

    @Test
    public void shouldCreateBudgetAndCalculateInitialMetrics() throws Exception {
        BudgetRequest bReq = new BudgetRequest();
        bReq.setCategory("Transportation");
        bReq.setMonth(9);
        bReq.setYear(2026);
        bReq.setAmount(200.0);

        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(200.0))
                .andExpect(jsonPath("$.data.spent").value(0.0))
                .andExpect(jsonPath("$.data.remaining").value(200.0))
                .andExpect(jsonPath("$.data.percentageUsed").value(0.0))
                .andExpect(jsonPath("$.data.status").value("normal"))
                .andExpect(jsonPath("$.data._id").isNotEmpty());
    }

    @Test
    public void shouldRejectDuplicateBudgetForTheSamePeriodAndCategory() throws Exception {
        BudgetRequest bReq = new BudgetRequest();
        bReq.setCategory("Food");
        bReq.setMonth(9);
        bReq.setYear(2026);
        bReq.setAmount(600.0);

        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void shouldReconcileSpentAmountDynamicallyAgainstExpensesInPeriod() throws Exception {
        // Add expense for User A in Sep 2026
        ExpenseRequest expReq = new ExpenseRequest();
        expReq.setAmount(350.0);
        expReq.setCategory("Food");
        expReq.setDescription("Monthly Groceries");
        expReq.setPaymentMethod("Credit Card");
        expReq.setDate(LocalDate.of(2026, 9, 10));

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/budgets/" + userABudgetId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(500.0))
                .andExpect(jsonPath("$.data.spent").value(350.0))
                .andExpect(jsonPath("$.data.remaining").value(150.0))
                .andExpect(jsonPath("$.data.percentageUsed").value(70.0))
                .andExpect(jsonPath("$.data.status").value("normal"));
    }

    @Test
    public void shouldSetStatusToWarningIfSpentIsOver80Percent() throws Exception {
        ExpenseRequest expReq = new ExpenseRequest();
        expReq.setAmount(420.0);
        expReq.setCategory("Food");
        expReq.setDescription("Big dining");
        expReq.setPaymentMethod("Credit Card");
        expReq.setDate(LocalDate.of(2026, 9, 12));

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/budgets/" + userABudgetId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spent").value(420.0))
                .andExpect(jsonPath("$.data.percentageUsed").value(84.0))
                .andExpect(jsonPath("$.data.status").value("warning"));
    }

    @Test
    public void shouldUpdateBudgetAmount() throws Exception {
        BudgetRequest updateReq = new BudgetRequest();
        updateReq.setCategory("Food");
        updateReq.setMonth(9);
        updateReq.setYear(2026);
        updateReq.setAmount(750.0);

        mockMvc.perform(put("/api/budgets/" + userABudgetId)
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(750.0));
    }

    @Test
    public void shouldDeleteBudget() throws Exception {
        mockMvc.perform(delete("/api/budgets/" + userABudgetId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void shouldEnforceTenantIsolation_UserBCannotViewOrModifyUserABudget() throws Exception {
        mockMvc.perform(get("/api/budgets/" + userABudgetId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        BudgetRequest updateReq = new BudgetRequest();
        updateReq.setAmount(1000.0);
        updateReq.setCategory("Food");
        updateReq.setMonth(9);
        updateReq.setYear(2026);

        mockMvc.perform(put("/api/budgets/" + userABudgetId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/budgets/" + userABudgetId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());
    }
}
