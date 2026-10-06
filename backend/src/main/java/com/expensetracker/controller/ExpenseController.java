package com.expensetracker.controller;

import com.expensetracker.dto.AnalyticsDto.*;
import com.expensetracker.dto.ApiResponse;
import com.expensetracker.dto.ApiResponse.PaginationMetadata;
import com.expensetracker.dto.ExpenseDto.ExpenseRequest;
import com.expensetracker.dto.ExpenseDto.ExpenseResponse;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
@Tag(name = "Expenses", description = "Expense CRUD, filtering, pagination, analytics, and CSV exports")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @Operation(summary = "Record a new expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse response = expenseService.createExpense(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense created successfully.", response));
    }

    @GetMapping
    @Operation(summary = "List paginated expenses with filters and sorting")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getExpenses(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int limit) {

        Map<String, Object> result = expenseService.getExpenses(
                userPrincipal.getId(), category, paymentMethod, startDate, endDate, minAmount, maxAmount, search, sort, page, limit);

        @SuppressWarnings("unchecked")
        List<ExpenseResponse> expenses = (List<ExpenseResponse>) result.get("expenses");
        PaginationMetadata pagination = (PaginationMetadata) result.get("pagination");

        return ResponseEntity.ok(ApiResponse.success("Expenses retrieved successfully.", expenses, pagination));
    }

    @GetMapping("/summary")
    @Operation(summary = "Calculate overall expense statistics")
    public ResponseEntity<ApiResponse<ExpenseSummaryResponse>> getExpenseSummary(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String category) {
        ExpenseSummaryResponse summary = expenseService.getExpenseSummary(userPrincipal.getId(), startDate, endDate, category);
        return ResponseEntity.ok(ApiResponse.success("Expense summary calculated successfully.", summary));
    }

    @GetMapping("/category-summary")
    @Operation(summary = "Get spending breakdown grouped by category")
    public ResponseEntity<ApiResponse<CategorySummaryResponse>> getCategorySummary(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        CategorySummaryResponse breakdown = expenseService.getCategorySummary(userPrincipal.getId(), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Category breakdown calculated successfully.", breakdown));
    }

    @GetMapping("/monthly-summary")
    @Operation(summary = "Get monthly spending trend for charts")
    public ResponseEntity<ApiResponse<List<MonthlySummaryItem>>> getMonthlySummary(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false, defaultValue = "6") int months) {
        List<MonthlySummaryItem> data = expenseService.getMonthlySummary(userPrincipal.getId(), months);
        return ResponseEntity.ok(ApiResponse.success("Monthly analytics calculated successfully.", data));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export expenses to RFC 4180 CSV file")
    public ResponseEntity<String> exportExpenses(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String search) {

        String csvData = expenseService.exportExpensesToCsv(
                userPrincipal.getId(), category, paymentMethod, startDate, endDate, minAmount, maxAmount, search);

        String filename = "expenses_export_" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get expense details by ID")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpenseById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        ExpenseResponse response = expenseService.getExpenseById(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Expense details retrieved.", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update expense by ID")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse response = expenseService.updateExpense(userPrincipal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Expense updated successfully.", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete expense by ID")
    public ResponseEntity<ApiResponse<Object>> deleteExpense(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        expenseService.deleteExpense(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Expense deleted successfully.", null));
    }
}
