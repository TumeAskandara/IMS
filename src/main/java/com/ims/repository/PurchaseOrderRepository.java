package com.ims.repository;

import com.ims.entity.PurchaseOrder;
import com.ims.enums.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    Page<PurchaseOrder> findBySupplierId(Long supplierId, Pageable pageable);

    Page<PurchaseOrder> findByBranchId(Long branchId, Pageable pageable);

    Page<PurchaseOrder> findByStatus(PurchaseOrderStatus status, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.orderDate BETWEEN :start AND :end")
    Page<PurchaseOrder> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po " +
            "LEFT JOIN FETCH po.items i " +
            "LEFT JOIN FETCH i.product " +
            "LEFT JOIN FETCH po.supplier " +
            "LEFT JOIN FETCH po.branch " +
            "LEFT JOIN FETCH po.orderedBy " +
            "LEFT JOIN FETCH po.approvedBy " +
            "WHERE po.id = :id")
    Optional<PurchaseOrder> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(po.totalAmount), 0) FROM PurchaseOrder po " +
            "WHERE po.status IN ('APPROVED', 'SHIPPED', 'PARTIALLY_RECEIVED', 'RECEIVED') " +
            "AND po.orderDate BETWEEN :start AND :end")
    BigDecimal getTotalPurchaseAmount(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query(value = "SELECT po FROM PurchaseOrder po " +
            "LEFT JOIN FETCH po.items i " +
            "LEFT JOIN FETCH i.product " +
            "LEFT JOIN FETCH po.supplier " +
            "LEFT JOIN FETCH po.branch " +
            "LEFT JOIN FETCH po.orderedBy " +
            "WHERE po.orderDate BETWEEN :start AND :end",
            countQuery = "SELECT COUNT(po) FROM PurchaseOrder po WHERE po.orderDate BETWEEN :start AND :end")
    Page<PurchaseOrder> findAllWithItemsByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
}
