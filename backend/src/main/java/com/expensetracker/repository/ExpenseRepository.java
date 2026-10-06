package com.expensetracker.repository;

import com.expensetracker.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(e.amount), 0.0) FROM Expense e " +
           "WHERE e.user.id = :userId " +
           "AND e.date >= :startDate AND e.date <= :endDate " +
           "AND (:category = 'All' OR e.category = :category)")
    Double getSumBetweenDatesForCategory(@Param("userId") Long userId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("category") String category);

    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0.0), COUNT(e) FROM Expense e " +
           "WHERE e.user.id = :userId " +
           "AND (:startDate IS NULL OR e.date >= :startDate) " +
           "AND (:endDate IS NULL OR e.date <= :endDate) " +
           "GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<Object[]> getCategorySummary(@Param("userId") Long userId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    @Query("SELECT YEAR(e.date), MONTH(e.date), COALESCE(SUM(e.amount), 0.0), COUNT(e) FROM Expense e " +
           "WHERE e.user.id = :userId AND e.date >= :startDate " +
           "GROUP BY YEAR(e.date), MONTH(e.date) " +
           "ORDER BY YEAR(e.date) ASC, MONTH(e.date) ASC")
    List<Object[]> getMonthlySummary(@Param("userId") Long userId,
                                     @Param("startDate") LocalDate startDate);
}
