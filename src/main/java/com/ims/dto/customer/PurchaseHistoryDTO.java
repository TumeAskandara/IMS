package com.ims.dto.customer;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseHistoryDTO {
    private Long saleId;
    private String invoiceNumber;
    private LocalDateTime saleDate;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private String paymentMethod;
    private String status;
    private Integer itemCount;
    private String branchName;
}
