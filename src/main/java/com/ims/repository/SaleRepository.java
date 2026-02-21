package com.ims.repository;

import com.ims.entity.Customer;
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

    // ==========================================
    // EAGER LOADING QUERIES (Prevent Lazy Init)
    // ==========================================

    /**
     * Find all sales with all relationships loaded
     * Prevents lazy initialization errors during JSON serialization
     */
    @Query(value = """
        SELECT DISTINCT s FROM Sale s
        LEFT JOIN FETCH s.saleItems si
        LEFT JOIN FETCH si.product
        LEFT JOIN FETCH s.branch
        LEFT JOIN FETCH s.seller
        LEFT JOIN FETCH s.customer
        WHERE s.isDeleted = false
        ORDER BY s.saleDate DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT s) FROM Sale s
        WHERE s.isDeleted = false
        """)
    Page<Sale> findAllWithItems(Pageable pageable);

    /**
     * Find sale by ID with all relationships loaded
     */
    @Query("""
        SELECT s FROM Sale s
        LEFT JOIN FETCH s.saleItems si
        LEFT JOIN FETCH si.product
        LEFT JOIN FETCH s.branch
        LEFT JOIN FETCH s.seller
        LEFT JOIN FETCH s.customer
        WHERE s.id = :id AND s.isDeleted = false
        """)
    Optional<Sale> findByIdWithItems(@Param("id") Long id);

    /**
     * Find sales by branch with all relationships loaded
     */
    @Query(value = """
        SELECT DISTINCT s FROM Sale s
        LEFT JOIN FETCH s.saleItems si
        LEFT JOIN FETCH si.product
        LEFT JOIN FETCH s.branch b
        LEFT JOIN FETCH s.seller
        LEFT JOIN FETCH s.customer
        WHERE b.id = :branchId AND s.isDeleted = false
        ORDER BY s.saleDate DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT s) FROM Sale s
        WHERE s.branch.id = :branchId AND s.isDeleted = false
        """)
    Page<Sale> findByBranchIdWithItems(@Param("branchId") Long branchId, Pageable pageable);

    /**
     * Find sales by status with all relationships loaded
     */
    @Query(value = """
        SELECT DISTINCT s FROM Sale s
        LEFT JOIN FETCH s.saleItems si
        LEFT JOIN FETCH si.product
        LEFT JOIN FETCH s.branch
        LEFT JOIN FETCH s.seller
        LEFT JOIN FETCH s.customer
        WHERE s.status = :status AND s.isDeleted = false
        ORDER BY s.saleDate DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT s) FROM Sale s
        WHERE s.status = :status AND s.isDeleted = false
        """)
    Page<Sale> findByStatusWithItems(@Param("status") SaleStatus status, Pageable pageable);

    /**
     * Find sales by customer with all relationships loaded
     */
    @Query(value = """
        SELECT DISTINCT s FROM Sale s
        LEFT JOIN FETCH s.saleItems si
        LEFT JOIN FETCH si.product
        LEFT JOIN FETCH s.branch
        LEFT JOIN FETCH s.seller
        LEFT JOIN FETCH s.customer c
        WHERE c = :customer AND s.isDeleted = false
        ORDER BY s.createdAt DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT s) FROM Sale s
        WHERE s.customer = :customer AND s.isDeleted = false
        """)
    Page<Sale> findByCustomerWithItems(@Param("customer") Customer customer, Pageable pageable);

    // ==========================================
    // SIMPLE QUERIES (No relationships needed)
    // ==========================================

    Optional<Sale> findByInvoiceNumber(String invoiceNumber);

    Page<Sale> findByBranchIdOrderBySaleDateDesc(Long branchId, Pageable pageable);

    List<Sale> findBySellerIdAndSaleDateBetween(Long sellerId, LocalDateTime start, LocalDateTime end);

    List<Sale> findByBranchIdAndSaleDateBetween(Long branchId, LocalDateTime start, LocalDateTime end);

    Page<Sale> findByStatusAndIsDeletedFalse(SaleStatus status, Pageable pageable);

    // ==========================================
    // AGGREGATION QUERIES
    // ==========================================

    @Query("""
        SELECT SUM(s.totalAmount) FROM Sale s 
        WHERE s.branch.id = :branchId 
        AND s.saleDate BETWEEN :start AND :end 
        AND s.status = 'COMPLETED'
        AND s.isDeleted = false
        """)
    BigDecimal getTotalSalesForBranch(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT COUNT(s) FROM Sale s 
        WHERE s.branch.id = :branchId 
        AND s.saleDate >= :today
        AND s.isDeleted = false
        """)
    Long getTodaySalesCount(
            @Param("branchId") Long branchId,
            @Param("today") LocalDateTime today
    );

    // ==========================================
    // CUSTOMER-RELATED QUERIES
    // ==========================================

    List<Sale> findByCustomerOrderByCreatedAtDesc(Customer customer);

    Page<Sale> findByCustomer(Customer customer, Pageable pageable);
}