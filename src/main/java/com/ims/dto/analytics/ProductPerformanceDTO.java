package com.ims.dto.analytics;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPerformanceDTO {
    private Long productId;
    private String productName;
    private String categoryName;
    private Integer quantitySold;
    private BigDecimal totalRevenue;
    private BigDecimal totalProfit;
    private Integer currentStock;
    private BigDecimal averageSellingPrice;
    private Long numberOfSales;
    private Double profitMargin;
}