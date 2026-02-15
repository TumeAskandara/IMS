package com.ims.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditAccountRequest {
    
    @NotBlank(message = "Customer name is required")
    private String customerName;
    
    private String customerPhone;
    
    private String customerEmail;
    
    private String customerAddress;
    
    private String idNumber;
    
    private BigDecimal creditLimit;
    
    private String notes;
}
