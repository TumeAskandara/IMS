package com.ims.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    
    @NotBlank(message = "SKU is required")
    private String sku;
    
    @NotBlank(message = "Product name is required")
    private String name;
    
    private String description;
    
    private Long categoryId;
    
    private String brand;
    
    private String model;
    
    private String barcode;
    
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    private BigDecimal unitPrice;
    
    private BigDecimal costPrice;
    
    private String unit;
    
    private Integer reorderLevel;
    
    private Integer minimumStock;
    
    private String notes;
    
    private String imageUrl;
    
    private Boolean isActive = true;
}
