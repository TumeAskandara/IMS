package com.ims.dto.returns;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleReturnDTO {
    private Long id;
    private String returnNumber;
    private String originalInvoiceNumber;
    private Long saleId;
    private String customerName;
    private String returnReason;
    private String refundMethod;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal refundAmount;
    private String processedBy;
    private Long branchId;
    private String branchName;
    private List<SaleReturnItemDTO> items;
    private String notes;
    private LocalDateTime returnDate;
    private LocalDateTime processedDate;
    private LocalDateTime createdAt;
}