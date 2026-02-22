package com.ims.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {
    
    private BigDecimal totalRevenue;
    private BigDecimal grossRevenue;
    private BigDecimal totalReturns;
    private Long totalReturnsCount;
    private Long totalSales;
    private BigDecimal totalStockValue;
    private BigDecimal totalOutstandingDebt;
    private BigDecimal overdueDebt;
    private Long activeDebtsCount;
    private Integer lowStockProductsCount;
    private Integer totalProducts;
    private Integer totalBranches;
}
