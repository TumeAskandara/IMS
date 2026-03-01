package com.ims.dto.purchaseorder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptItemRequest {

    @NotNull(message = "PO Item ID is required")
    private Long poItemId;

    @NotNull(message = "Quantity received is required")
    @Min(value = 0, message = "Quantity received cannot be negative")
    private Integer quantityReceived;

    @Min(value = 0, message = "Quantity damaged cannot be negative")
    private Integer quantityDamaged = 0;
}
