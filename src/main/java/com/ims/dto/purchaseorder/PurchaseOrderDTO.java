package com.ims.dto.purchaseorder;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderDTO {
    private Long id;
    private String poNumber;
    private Long supplierId;
    private String supplierName;
    private Long branchId;
    private String branchName;
    private String orderedByName;
    private String approvedByName;
    private LocalDateTime orderDate;
    private LocalDate expectedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal shippingCost;
    private BigDecimal totalAmount;
    private String notes;
    private String cancellationReason;
    private List<PurchaseOrderItemDTO> items;
    private LocalDateTime createdAt;
}
