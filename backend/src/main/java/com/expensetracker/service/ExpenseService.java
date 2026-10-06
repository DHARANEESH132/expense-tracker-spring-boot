package com.expensetracker.service;

import com.expensetracker.dto.AnalyticsDto.*;
import com.expensetracker.dto.ApiResponse.PaginationMetadata;
import com.expensetracker.dto.ExpenseDto.ExpenseRequest;
import com.expensetracker.dto.ExpenseDto.ExpenseResponse;
import com.expensetracker.entity.Expense;
import com.expensetracker.entity.User;
import com.expensetracker.exception.AppException;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.util.CsvExporter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public ExpenseService(ExpenseRepository expenseRepository, UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    private ExpenseResponse toResponse(Expense exp) {
        return new ExpenseResponse(
                String.valueOf(exp.getId()),
                exp.getAmount(),
                exp.getCategory(),
                exp.getDescription(),
                exp.getPaymentMethod(),
                exp.getDate(),
                exp.getNotes(),
                exp.getCreatedAt(),
                exp.getUpdatedAt()
        );
    }

    private Specification<Expense> buildSpecification(Long userId, String category, String paymentMethod,
                                                      LocalDate startDate, LocalDate endDate,
                                                      Double minAmount, Double maxAmount, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (category != null && !category.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("category"), category.trim()));
            }

            if (paymentMethod != null && !paymentMethod.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod.trim()));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), endDate));
            }

            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }

            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            if (search != null && !search.trim().isEmpty()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate descMatch = cb.like(cb.lower(root.get("description")), term);
                Predicate notesMatch = cb.like(cb.lower(root.get("notes")), term);
                predicates.add(cb.or(descMatch, notesMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional
    public ExpenseResponse createExpense(Long userId, ExpenseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found.", HttpStatus.NOT_FOUND));

        Expense expense = new Expense(
                user,
                request.getAmount(),
                request.getCategory(),
                request.getDescription().trim(),
                request.getPaymentMethod(),
                request.getDate() != null ? request.getDate() : LocalDate.now(),
                request.getNotes() != null ? request.getNotes().trim() : ""
        );

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getExpenses(Long userId, String category, String paymentMethod,
                                           LocalDate startDate, LocalDate endDate,
                                           Double minAmount, Double maxAmount, String search,
                                           String sort, int page, int limit) {

        int validPage = Math.max(1, page);
        int validLimit = Math.min(100, Math.max(1, limit));

        Sort sortObj = switch (sort != null ? sort : "newest") {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "date").and(Sort.by(Sort.Direction.ASC, "id"));
            case "highest" -> Sort.by(Sort.Direction.DESC, "amount");
            case "lowest" -> Sort.by(Sort.Direction.ASC, "amount");
            default -> Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id"));
        };

        Pageable pageable = PageRequest.of(validPage - 1, validLimit, sortObj);
        Specification<Expense> spec = buildSpecification(userId, category, paymentMethod, startDate, endDate, minAmount, maxAmount, search);

        Page<Expense> expensePage = expenseRepository.findAll(spec, pageable);

        List<ExpenseResponse> items = expensePage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        PaginationMetadata pagination = new PaginationMetadata(
                validPage,
                Math.max(1, expensePage.getTotalPages()),
                expensePage.getTotalElements(),
                validLimit
        );

        Map<String, Object> result = new HashMap<>();
        result.put("expenses", items);
        result.put("pagination", pagination);
        return result;
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long userId, Long expenseId) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new AppException("Expense not found or you do not have permission to access it.", HttpStatus.NOT_FOUND));
        return toResponse(expense);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long userId, Long expenseId, ExpenseRequest request) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new AppException("Expense not found or you do not have permission to modify it.", HttpStatus.NOT_FOUND));

        if (request.getAmount() != null) expense.setAmount(request.getAmount());
        if (request.getCategory() != null) expense.setCategory(request.getCategory());
        if (request.getDescription() != null) expense.setDescription(request.getDescription().trim());
        if (request.getPaymentMethod() != null) expense.setPaymentMethod(request.getPaymentMethod());
        if (request.getDate() != null) expense.setDate(request.getDate());
        if (request.getNotes() != null) expense.setNotes(request.getNotes().trim());

        Expense updated = expenseRepository.save(expense);
        return toResponse(updated);
    }

    @Transactional
    public void deleteExpense(Long userId, Long expenseId) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new AppException("Expense not found or you do not have permission to delete it.", HttpStatus.NOT_FOUND));
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getExpenseSummary(Long userId, LocalDate startDate, LocalDate endDate, String category) {
        Specification<Expense> spec = buildSpecification(userId, category, null, startDate, endDate, null, null, null);
        List<Expense> expenses = expenseRepository.findAll(spec);

        if (expenses.isEmpty()) {
            return new ExpenseSummaryResponse(0.0, 0L, 0.0, 0.0, 0.0);
        }

        double total = 0.0;
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;

        for (Expense e : expenses) {
            double amt = e.getAmount();
            total += amt;
            if (amt > max) max = amt;
            if (amt < min) min = amt;
        }

        long count = expenses.size();
        double avg = Math.round((total / count) * 100.0) / 100.0;
        double roundedTotal = Math.round(total * 100.0) / 100.0;

        return new ExpenseSummaryResponse(roundedTotal, count, avg, max, min);
    }

    @Transactional(readOnly = true)
    public CategorySummaryResponse getCategorySummary(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> rows = expenseRepository.getCategorySummary(userId, startDate, endDate);

        double grandTotal = 0.0;
        List<CategorySummaryItem> items = new ArrayList<>();

        for (Object[] row : rows) {
            String cat = (String) row[0];
            Double total = ((Number) row[1]).doubleValue();
            Long count = ((Number) row[2]).longValue();
            grandTotal += total;
            items.add(new CategorySummaryItem(cat, Math.round(total * 100.0) / 100.0, count, 0.0));
        }

        for (CategorySummaryItem item : items) {
            if (grandTotal > 0) {
                double pct = Math.round((item.getTotal() / grandTotal) * 1000.0) / 10.0;
                item.setPercentage(pct);
            }
        }

        return new CategorySummaryResponse(Math.round(grandTotal * 100.0) / 100.0, items);
    }

    @Transactional(readOnly = true)
    public List<MonthlySummaryItem> getMonthlySummary(Long userId, int monthsCount) {
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.minusMonths(monthsCount - 1).withDayOfMonth(1);

        List<Object[]> rows = expenseRepository.getMonthlySummary(userId, startDate);
        Map<String, double[]> monthlyMap = new HashMap<>();

        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            double total = ((Number) row[2]).doubleValue();
            long count = ((Number) row[3]).longValue();
            monthlyMap.put(year + "-" + month, new double[]{total, (double) count});
        }

        List<MonthlySummaryItem> result = new ArrayList<>();
        for (int i = monthsCount - 1; i >= 0; i--) {
            LocalDate d = now.minusMonths(i);
            int yr = d.getYear();
            int mo = d.getMonthValue();
            String key = yr + "-" + mo;
            String label = Month.of(mo).getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + yr;

            double[] val = monthlyMap.get(key);
            double total = val != null ? Math.round(val[0] * 100.0) / 100.0 : 0.0;
            long count = val != null ? (long) val[1] : 0L;

            result.add(new MonthlySummaryItem(yr, mo, label, total, count));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public String exportExpensesToCsv(Long userId, String category, String paymentMethod,
                                      LocalDate startDate, LocalDate endDate,
                                      Double minAmount, Double maxAmount, String search) {
        Specification<Expense> spec = buildSpecification(userId, category, paymentMethod, startDate, endDate, minAmount, maxAmount, search);
        Sort sort = Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id"));
        List<Expense> expenses = expenseRepository.findAll(spec, sort);

        List<ExpenseResponse> responses = expenses.stream().map(this::toResponse).collect(Collectors.toList());
        return CsvExporter.exportToCsv(responses);
    }
}
