package com.ims.dto.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryDTO {
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Long totalSales;
    private BigDecimal totalRevenue;
    private BigDecimal totalProfit;
    private BigDecimal averageSaleValue;
    private Integer totalItemsSold;
    private Long uniqueCustomers;
    private BigDecimal cashSales;
    private BigDecimal creditSales;
    private BigDecimal cardSales;
    private BigDecimal totalCost;
    private Double profitMargin; // Percentage
    private BigDecimal totalExpenses; // ADD THIS
}