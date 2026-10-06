package com.expensetracker.controller;

import com.expensetracker.dto.ApiResponse;
import com.expensetracker.dto.BudgetDto.BudgetRequest;
import com.expensetracker.dto.BudgetDto.BudgetResponse;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@Tag(name = "Budgets", description = "Budget management and spending progress reconciliation")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    @Operation(summary = "Create a new monthly or category budget")
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody BudgetRequest request) {
        BudgetResponse response = budgetService.createBudget(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Budget created successfully.", response));
    }

    @GetMapping
    @Operation(summary = "List budgets with dynamic spending calculation")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgets(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        List<BudgetResponse> response = budgetService.getBudgets(userPrincipal.getId(), month, year);
        return ResponseEntity.ok(ApiResponse.success("Budgets retrieved successfully.", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get budget details by ID")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudgetById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        BudgetResponse response = budgetService.getBudgetById(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Budget details retrieved.", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update budget by ID")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {
        BudgetResponse response = budgetService.updateBudget(userPrincipal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Budget updated successfully.", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete budget by ID")
    public ResponseEntity<ApiResponse<Object>> deleteBudget(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        budgetService.deleteBudget(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Budget deleted successfully.", null));
    }
}
