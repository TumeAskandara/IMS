package com.ims.dto.analytics;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryValuationDTO {
    private Long totalProducts;
    private Long totalCategories;
    private Integer totalQuantity;
    private BigDecimal totalValue;
    private BigDecimal totalCost;
    private Long lowStockProducts;
    private Long outOfStockProducts;
    private BigDecimal averageProductValue;
    private String mostValuableProduct;
    private BigDecimal mostValuableProductValue;
}