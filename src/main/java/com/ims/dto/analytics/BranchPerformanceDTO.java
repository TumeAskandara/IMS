package com.ims.dto.analytics;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchPerformanceDTO {
    private Long branchId;
    private String branchName;
    private Long totalSales;
    private BigDecimal totalRevenue;
    private BigDecimal totalProfit;
    private Integer totalProducts;
    private Integer totalInventoryValue;
    private Long activeCustomers;
    private Integer numberOfEmployees;
    private BigDecimal averageSaleValue;
    private Double profitMargin;
    private String topProduct;
}