package com.expensetracker.repository;

import com.expensetracker.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    List<Budget> findByUserIdOrderByYearDescMonthDescCategoryAsc(Long userId);

    List<Budget> findByUserIdAndMonthAndYearOrderByCategoryAsc(Long userId, Integer month, Integer year);

    Optional<Budget> findByUserIdAndCategoryAndMonthAndYear(Long userId, String category, Integer month, Integer year);

    boolean existsByUserIdAndCategoryAndMonthAndYear(Long userId, String category, Integer month, Integer year);
}
