package com.ims.dto.request;

import com.ims.enums.StockMovementType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotNull(message = "Quantity is required")
    private Integer quantity;
    
    @NotNull(message = "Movement type is required")
    private StockMovementType movementType;
    
    private String notes;
}
