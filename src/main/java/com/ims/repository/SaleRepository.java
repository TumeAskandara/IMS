package com.ims.repository;

import com.ims.entity.Sale;
import com.ims.enums.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    
    Optional<Sale> findByInvoiceNumber(String invoiceNumber);
    
    Page<Sale> findByBranchIdOrderBySaleDateDesc(Long branchId, Pageable pageable);
    
    List<Sale> findBySellerIdAndSaleDateBetween(Long sellerId, LocalDateTime start, LocalDateTime end);
    
    List<Sale> findByBranchIdAndSaleDateBetween(Long branchId, LocalDateTime start, LocalDateTime end);
    
    Page<Sale> findByStatusAndIsDeletedFalse(SaleStatus status, Pageable pageable);
    
    @Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.branch.id = :branchId AND " +
           "s.saleDate BETWEEN :start AND :end AND s.status = 'COMPLETED'")
    BigDecimal getTotalSalesForBranch(@Param("branchId") Long branchId, 
                                      @Param("start") LocalDateTime start, 
                                      @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.branch.id = :branchId AND s.saleDate >= :today")
    Long getTodaySalesCount(@Param("branchId") Long branchId, @Param("today") LocalDateTime today);
}
