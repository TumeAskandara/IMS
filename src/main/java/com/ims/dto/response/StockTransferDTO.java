package com.ims.dto.response;

import com.ims.enums.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferDTO {
    private Long id;
    private String transferNumber;
    private Long sourceBranchId;
    private String sourceBranchName;
    private Long destinationBranchId;
    private String destinationBranchName;
    private TransferStatus status;
    private String requestedByName;
    private LocalDateTime requestDate;
    private String approvedByName;
    private LocalDateTime approvalDate;
    private LocalDateTime shipDate;
    private LocalDateTime receiveDate;
    private String rejectionReason;
    private String notes;
    private List<TransferItemDTO> transferItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}