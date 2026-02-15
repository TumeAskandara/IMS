package com.ims.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleItemRequest {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotNull(message = "Quantity is required")
    private Integer quantity;
    
    @NotNull(message = "Unit price is required")
    private BigDecimal unitPrice;
    
    private BigDecimal discountAmount = BigDecimal.ZERO;
}
