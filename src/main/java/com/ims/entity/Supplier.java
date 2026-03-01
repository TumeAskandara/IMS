package com.ims.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ims.enums.SupplierPaymentTerms;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Supplier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_terms")
    private SupplierPaymentTerms paymentTerms;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column
    private Double rating;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
