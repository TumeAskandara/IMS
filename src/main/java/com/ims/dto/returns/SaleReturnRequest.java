package com.ims.dto.returns;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleReturnRequest {

    @NotNull(message = "Sale ID is required")
    private Long saleId;

    @NotBlank(message = "Return reason is required")
    @Size(max = 500, message = "Return reason cannot exceed 500 characters")
    private String returnReason;

    @NotNull(message = "Refund method is required")
    private String refundMethod; // CASH, CREDIT, ORIGINAL_METHOD

    @NotNull(message = "Return items are required")
    @Size(min = 1, message = "At least one item must be returned")
    @Valid
    private List<ReturnItemRequest> items;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemRequest {

        @NotNull(message = "Sale item ID is required")
        private Long saleItemId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @NotBlank(message = "Return reason is required")
        private String returnReason;

        @NotBlank(message = "Item condition is required")
        private String condition; // DAMAGED, DEFECTIVE, UNOPENED, USED
    }
}