package com.ims.dto.purchaseorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderRequest {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    private LocalDate expectedDeliveryDate;

    private BigDecimal taxAmount;

    private BigDecimal shippingCost;

    private String notes;

    @NotNull(message = "At least one item is required")
    @Size(min = 1, message = "At least one item is required")
    @Valid
    private List<PurchaseOrderItemRequest> items;
}
