package com.ims.repository;

import com.ims.entity.StockTransfer;
import com.ims.enums.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    
    Optional<StockTransfer> findByTransferNumber(String transferNumber);
    
    Page<StockTransfer> findBySourceBranchIdOrDestinationBranchId(Long sourceBranchId, Long destinationBranchId, Pageable pageable);
    
    List<StockTransfer> findByStatus(TransferStatus status);
    
    Page<StockTransfer> findByStatusAndIsDeletedFalse(TransferStatus status, Pageable pageable);
    
    List<StockTransfer> findBySourceBranchIdAndStatus(Long sourceBranchId, TransferStatus status);
    
    List<StockTransfer> findByDestinationBranchIdAndStatus(Long destinationBranchId, TransferStatus status);
}
