package com.ims.dto.request;

import com.ims.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequest {
    
    @NotNull(message = "Branch ID is required")
    private Long branchId;

    private Long customerId;

    private String customerName;
    
    private String customerPhone;
    
    private String customerEmail;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    @NotNull(message = "Amount paid is required")
    private BigDecimal amountPaid;
    
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    private String notes;
    
    @NotNull(message = "Sale items are required")
    private List<SaleItemRequest> items;
    
    // For credit sales
    private Long creditAccountId;
    private String dueDate; // LocalDate as string
}
