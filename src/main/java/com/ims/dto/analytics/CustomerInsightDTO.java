package com.ims.dto.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerInsightDTO {
    private Long customerId;
    private String customerName;
    private String customerType;
    private BigDecimal lifetimeValue;
    private Integer totalPurchases;
    private BigDecimal averagePurchaseValue;
    private BigDecimal currentDebt;
    private LocalDateTime lastPurchaseDate;
    private Integer daysSinceLastPurchase;
    private String preferredProduct;
}