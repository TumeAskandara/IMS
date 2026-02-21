package com.ims.dto.analytics;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitAnalysisDTO {
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal grossProfit;
    private BigDecimal netProfit;
    private Double grossProfitMargin;
    private Double netProfitMargin;
    private BigDecimal operatingExpenses;
    private String mostProfitableProduct;
    private BigDecimal mostProfitableProductProfit;
    private String leastProfitableProduct;
    private BigDecimal leastProfitableProductProfit;
    private String mostProfitableBranch;
    private BigDecimal mostProfitableBranchProfit;
}