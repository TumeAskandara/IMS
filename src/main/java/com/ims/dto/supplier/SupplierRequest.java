package com.ims.dto.supplier;

import com.ims.enums.SupplierPaymentTerms;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRequest {

    @NotBlank(message = "Supplier name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 100)
    private String contactPerson;

    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String phone;

    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    private SupplierPaymentTerms paymentTerms;

    private Integer leadTimeDays;

    private Double rating;
}
