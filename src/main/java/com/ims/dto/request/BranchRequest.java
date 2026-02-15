package com.ims.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchRequest {
    
    @NotBlank(message = "Branch code is required")
    private String code;
    
    @NotBlank(message = "Branch name is required")
    private String name;
    
    private String address;
    
    private String phoneNumber;
    
    private String city;
    
    private String country;
    
    private Boolean isActive = true;
}
