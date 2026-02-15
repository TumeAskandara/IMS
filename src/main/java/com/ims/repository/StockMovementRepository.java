package com.ims.repository;

import com.ims.entity.StockMovement;
import com.ims.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    
    List<StockMovement> findByProductId(Long productId);
    
    List<StockMovement> findByBranchId(Long branchId);
    
    Page<StockMovement> findByBranchIdOrderByCreatedAtDesc(Long branchId, Pageable pageable);
    
    List<StockMovement> findByMovementType(StockMovementType movementType);
    
    List<StockMovement> findByBranchIdAndCreatedAtBetween(Long branchId, LocalDateTime start, LocalDateTime end);
    
    List<StockMovement> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}
