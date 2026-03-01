package com.ims.dto.supplier;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDTO {
    private Long id;
    private String code;
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private String paymentTerms;
    private Integer leadTimeDays;
    private Double rating;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
