package com.ims.repository;

import com.ims.entity.Expense;
import com.ims.enums.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByBranchIdAndIsDeletedFalseOrderByExpenseDateDesc(Long branchId, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.branch.id = :branchId " +
            "AND e.expenseDate BETWEEN :startDate AND :endDate " +
            "AND e.isDeleted = false")
    List<Expense> findByBranchAndDateRange(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.branch.id = :branchId " +
            "AND e.expenseDate = :date AND e.isDeleted = false")
    BigDecimal getTotalExpensesForDate(@Param("branchId") Long branchId, @Param("date") LocalDate date);
}