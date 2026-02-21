package com.ims.repository;

import com.ims.entity.SaleReturn;
import com.ims.enums.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {

    Optional<SaleReturn> findByReturnNumber(String returnNumber);

    Page<SaleReturn> findByBranchIdOrderByCreatedAtDesc(Long branchId, Pageable pageable);

    Page<SaleReturn> findByStatusOrderByCreatedAtDesc(ReturnStatus status, Pageable pageable);

    List<SaleReturn> findBySaleId(Long saleId);

    @Query("SELECT sr FROM SaleReturn sr WHERE sr.returnDate BETWEEN :startDate AND :endDate")
    List<SaleReturn> findByReturnDateBetween(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    @Query("SELECT sr FROM SaleReturn sr WHERE sr.branch.id = :branchId AND " +
            "sr.returnDate BETWEEN :startDate AND :endDate")
    List<SaleReturn> findByBranchAndDateBetween(@Param("branchId") Long branchId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(sr) FROM SaleReturn sr WHERE sr.status = :status")
    Long countByStatus(@Param("status") ReturnStatus status);

    List<SaleReturn> findByBranchIdAndReturnDateBetween(Long branchId, LocalDateTime startDate, LocalDateTime endDate);


}