package com.ims.dto.customer;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {
    
    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @Pattern(regexp = "^[0-9+\\-\\s()]*$", message = "Invalid phone number format")
    private String phone;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;
    
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;
    
    private String taxId;
    
    @NotNull(message = "Customer type is required")
    private String customerType; // RETAIL, WHOLESALE, CORPORATE, GOVERNMENT, VIP
    
    @DecimalMin(value = "0.0", message = "Credit limit cannot be negative")
    private BigDecimal creditLimit;
    
    @NotNull(message = "Branch ID is required")
    private Long branchId;
    
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}
