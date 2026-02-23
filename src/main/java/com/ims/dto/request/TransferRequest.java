package com.ims.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    
    @NotNull(message = "Source branch ID is required")
    private Long sourceBranchId;
    
    @NotNull(message = "Destination branch ID is required")
    private Long destinationBranchId;
    
    @NotNull(message = "Transfer items are required")
    private List<TransferItemRequest> items;
    
    private String notes;

    /**
     * When true, the transfer is immediately completed in a single step:
     * stock is deducted from source and added to destination without
     * requiring separate approve/ship/receive steps.
     */
    private Boolean directTransfer = false;
}
