package com.ims.dto.customer;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
    private Long id;
    private String customerId;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String taxId;
    private String customerType;
    private String status;
    private BigDecimal creditLimit;
    private BigDecimal currentDebt;
    private BigDecimal availableCredit;
    private BigDecimal lifetimeValue;
    private Integer totalPurchases;
    private LocalDateTime lastPurchaseDate;
    private String branchName;
    private String notes;
    private LocalDateTime createdAt;
}
