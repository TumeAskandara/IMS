package com.ims.dto.purchaseorder;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Integer quantityOrdered;
    private Integer quantityReceived;
    private Integer quantityDamaged;
    private BigDecimal unitCost;
    private BigDecimal lineTotal;
}
