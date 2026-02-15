package com.ims.dto.request;

import com.ims.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    private String password;
    
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String phoneNumber;
    
    @NotNull(message = "Role is required")
    private Role role;
    
    private Long branchId;
    
    private Boolean isActive = true;
}
