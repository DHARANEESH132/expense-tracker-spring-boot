package com.expensetracker;

import com.expensetracker.dto.AuthDto.RegisterRequest;
import com.expensetracker.dto.ExpenseDto.ExpenseRequest;
import com.fasterxml.jackson.databind.JsonNode;
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
public class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userAToken;
    private String userBToken;
    private String userAExpenseId;

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

        // Create an expense for User A
        ExpenseRequest expReq = new ExpenseRequest();
        expReq.setAmount(50.75);
        expReq.setCategory("Food");
        expReq.setDescription("Dinner with friends");
        expReq.setPaymentMethod("Credit Card");
        expReq.setDate(LocalDate.of(2026, 9, 1));
        expReq.setNotes("Italian restaurant");

        MvcResult expRes = mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expReq)))
                .andExpect(status().isCreated())
                .andReturn();
        userAExpenseId = objectMapper.readTree(expRes.getResponse().getContentAsString()).path("data").path("id").asText();
    }

    @Test
    public void shouldCreateExpenseWithValidInput() throws Exception {
        ExpenseRequest expReq = new ExpenseRequest();
        expReq.setAmount(25.0);
        expReq.setCategory("Transportation");
        expReq.setDescription("Subway pass");
        expReq.setPaymentMethod("Cash");

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(25.0))
                .andExpect(jsonPath("$.data.category").value("Transportation"))
                .andExpect(jsonPath("$.data._id").isNotEmpty()); // Ensures _id field exists for frontend!
    }

    @Test
    public void shouldGetExpensesForAuthenticatedUserOnly() throws Exception {
        mockMvc.perform(get("/api/expenses")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.pagination.totalItems").value(1));
    }

    @Test
    public void shouldSupportCategoryFiltering() throws Exception {
        ExpenseRequest expReq = new ExpenseRequest();
        expReq.setAmount(15.0);
        expReq.setCategory("Transportation");
        expReq.setDescription("Bus ticket");
        expReq.setPaymentMethod("Cash");

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/expenses?category=Transportation")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].category").value("Transportation"));
    }

    @Test
    public void shouldUpdateExistingExpense() throws Exception {
        ExpenseRequest updateReq = new ExpenseRequest();
        updateReq.setAmount(60.0);
        updateReq.setCategory("Food");
        updateReq.setDescription("Updated Dinner with dessert");
        updateReq.setPaymentMethod("Credit Card");

        mockMvc.perform(put("/api/expenses/" + userAExpenseId)
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(60.0))
                .andExpect(jsonPath("$.data.description").value("Updated Dinner with dessert"));
    }

    @Test
    public void shouldDeleteExistingExpense() throws Exception {
        mockMvc.perform(delete("/api/expenses/" + userAExpenseId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/expenses/" + userAExpenseId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldEnforceTenantIsolation_UserBCannotViewUserAExpense() throws Exception {
        mockMvc.perform(get("/api/expenses/" + userAExpenseId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void shouldEnforceTenantIsolation_UserBCannotUpdateUserAExpense() throws Exception {
        ExpenseRequest updateReq = new ExpenseRequest();
        updateReq.setAmount(9999.0);
        updateReq.setCategory("Food");
        updateReq.setDescription("Hacked");
        updateReq.setPaymentMethod("Cash");

        mockMvc.perform(put("/api/expenses/" + userAExpenseId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldEnforceTenantIsolation_UserBCannotDeleteUserAExpense() throws Exception {
        mockMvc.perform(delete("/api/expenses/" + userAExpenseId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // Verify User A still has their expense
        mockMvc.perform(get("/api/expenses/" + userAExpenseId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldCalculateExpenseSummaryAccurately() throws Exception {
        ExpenseRequest expReq = new ExpenseRequest();
        expReq.setAmount(100.0);
        expReq.setCategory("Shopping");
        expReq.setDescription("Shoes");
        expReq.setPaymentMethod("Credit Card");

        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/expenses/summary")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expenseCount").value(2))
                .andExpect(jsonPath("$.data.totalExpenses").value(150.75))
                .andExpect(jsonPath("$.data.highestExpense").value(100.0))
                .andExpect(jsonPath("$.data.lowestExpense").value(50.75));
    }
}
