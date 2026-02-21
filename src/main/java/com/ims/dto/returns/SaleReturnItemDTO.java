package com.ims.dto.returns;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleReturnItemDTO {
    private Long id;
    private String productName;
    private String productSku;
    private Long productId;
    private Integer quantityReturned;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String returnReason;
    private String condition;
}