package com.ims.repository;

import com.ims.entity.Debt;
import com.ims.enums.DebtStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {
    
    Optional<Debt> findBySaleId(Long saleId);
    
    List<Debt> findByCreditAccountId(Long creditAccountId);
    
    Page<Debt> findByCreditAccountId(Long creditAccountId, Pageable pageable);
    
    List<Debt> findByStatus(DebtStatus status);
    
    Page<Debt> findByStatusAndIsDeletedFalse(DebtStatus status, Pageable pageable);
    
    @Query("SELECT d FROM Debt d WHERE d.dueDate < :today AND d.status IN ('PENDING', 'PARTIALLY_PAID')")
    List<Debt> findOverdueDebts(@Param("today") LocalDate today);
    
    @Query("SELECT SUM(d.balanceDue) FROM Debt d WHERE d.status IN ('PENDING', 'PARTIALLY_PAID')")
    BigDecimal getTotalOutstandingDebt();
    
    @Query("SELECT SUM(d.balanceDue) FROM Debt d WHERE d.status = 'OVERDUE'")
    BigDecimal getTotalOverdueDebt();
    
    @Query("SELECT COUNT(d) FROM Debt d WHERE d.status IN ('PENDING', 'PARTIALLY_PAID')")
    Long getActiveDebtsCount();

    @Query("SELECT d FROM Debt d WHERE d.dueDate < :today AND d.status IN ('PENDING', 'PARTIALLY_PAID')")
    List<Debt> findNewlyOverdueDebts(@Param("today") LocalDate today);

    // Branch-filtered queries (debt -> sale -> branch)
    @Query("SELECT d FROM Debt d WHERE d.sale.branch.id = :branchId AND d.isDeleted = false")
    Page<Debt> findByBranch(@Param("branchId") Long branchId, Pageable pageable);

    @Query("SELECT d FROM Debt d WHERE d.sale.branch.id = :branchId AND d.dueDate < :today AND d.status IN ('PENDING', 'PARTIALLY_PAID')")
    List<Debt> findOverdueDebtsByBranch(@Param("branchId") Long branchId, @Param("today") LocalDate today);

    @Query("SELECT d FROM Debt d WHERE d.sale.branch.id = :branchId AND d.status = :status AND d.isDeleted = false")
    Page<Debt> findByStatusAndBranch(@Param("status") DebtStatus status, @Param("branchId") Long branchId, Pageable pageable);

    @Query("SELECT SUM(d.balanceDue) FROM Debt d WHERE d.sale.branch.id = :branchId AND d.status IN ('PENDING', 'PARTIALLY_PAID')")
    BigDecimal getTotalOutstandingDebtForBranch(@Param("branchId") Long branchId);

    @Query("SELECT SUM(d.balanceDue) FROM Debt d WHERE d.sale.branch.id = :branchId AND d.status = 'OVERDUE'")
    BigDecimal getTotalOverdueDebtForBranch(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(d) FROM Debt d WHERE d.sale.branch.id = :branchId AND d.status IN ('PENDING', 'PARTIALLY_PAID')")
    Long getActiveDebtsCountForBranch(@Param("branchId") Long branchId);
}
