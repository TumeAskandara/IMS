package com.ims.dto.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTrendDTO {
    private LocalDate date;
    private Long numberOfSales;
    private BigDecimal totalRevenue;
    private BigDecimal totalProfit;
    private Integer itemsSold;
    private BigDecimal averageSaleValue;
}