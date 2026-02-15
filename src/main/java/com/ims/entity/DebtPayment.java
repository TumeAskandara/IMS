package com.ims.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ims.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "debt_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtPayment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_id", nullable = false)
    @JsonBackReference  // THIS BREAKS THE CIRCULAR REFERENCE
    private Debt debt;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "received_by")
    private User receivedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
