package com.expensetracker.service;

import com.expensetracker.dto.BudgetDto.BudgetRequest;
import com.expensetracker.dto.BudgetDto.BudgetResponse;
import com.expensetracker.entity.Budget;
import com.expensetracker.entity.User;
import com.expensetracker.exception.AppException;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         ExpenseRepository expenseRepository,
                         UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    private BudgetResponse enrichBudget(Long userId, Budget b) {
        LocalDate startOfMonth = LocalDate.of(b.getYear(), b.getMonth(), 1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

        Double rawSpent = expenseRepository.getSumBetweenDatesForCategory(userId, startOfMonth, endOfMonth, b.getCategory());
        double spent = rawSpent != null ? Math.round(rawSpent * 100.0) / 100.0 : 0.0;
        double remaining = Math.round((b.getAmount() - spent) * 100.0) / 100.0;
        double percentageUsed = b.getAmount() > 0 ? Math.round((spent / b.getAmount()) * 1000.0) / 10.0 : 0.0;

        String status = "normal";
        if (percentageUsed >= 100.0) {
            status = "exceeded";
        } else if (percentageUsed >= 80.0) {
            status = "warning";
        }

        return new BudgetResponse(
                String.valueOf(b.getId()),
                b.getCategory(),
                b.getMonth(),
                b.getYear(),
                b.getAmount(),
                spent,
                remaining,
                percentageUsed,
                status,
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }

    @Transactional
    public BudgetResponse createBudget(Long userId, BudgetRequest request) {
        boolean exists = budgetRepository.existsByUserIdAndCategoryAndMonthAndYear(
                userId, request.getCategory(), request.getMonth(), request.getYear());

        if (exists) {
            throw new AppException(
                    "A budget for '" + request.getCategory() + "' for " + request.getMonth() + "/" + request.getYear() +
                    " already exists. You can update it instead.", HttpStatus.CONFLICT);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found.", HttpStatus.NOT_FOUND));

        Budget budget = new Budget(
                user,
                request.getCategory(),
                request.getMonth(),
                request.getYear(),
                request.getAmount()
        );

        Budget saved = budgetRepository.save(budget);
        return enrichBudget(userId, saved);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(Long userId, Integer month, Integer year) {
        List<Budget> budgets;
        if (month != null && year != null) {
            budgets = budgetRepository.findByUserIdAndMonthAndYearOrderByCategoryAsc(userId, month, year);
        } else {
            budgets = budgetRepository.findByUserIdOrderByYearDescMonthDescCategoryAsc(userId);
        }

        return budgets.stream()
                .map(b -> enrichBudget(userId, b))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new AppException("Budget not found or you do not have permission to view it.", HttpStatus.NOT_FOUND));
        return enrichBudget(userId, budget);
    }

    @Transactional
    public BudgetResponse updateBudget(Long userId, Long budgetId, BudgetRequest request) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new AppException("Budget not found or you do not have permission to modify it.", HttpStatus.NOT_FOUND));

        if (request.getAmount() != null) budget.setAmount(request.getAmount());
        if (request.getCategory() != null) budget.setCategory(request.getCategory());
        if (request.getMonth() != null) budget.setMonth(request.getMonth());
        if (request.getYear() != null) budget.setYear(request.getYear());

        Budget updated = budgetRepository.save(budget);
        return enrichBudget(userId, updated);
    }

    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new AppException("Budget not found or you do not have permission to delete it.", HttpStatus.NOT_FOUND));
        budgetRepository.delete(budget);
    }
}
