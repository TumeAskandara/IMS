package com.ims.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ims.enums.DebtStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "debts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Debt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "credit_account_id", nullable = false)
    private CreditAccount creditAccount;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "amount_paid", precision = 12, scale = 2, nullable = false)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "balance_due", precision = 12, scale = 2, nullable = false)
    private BigDecimal balanceDue;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebtStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "debt", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference  // THIS BREAKS THE CIRCULAR REFERENCE
    @Builder.Default
    private List<DebtPayment> payments = new ArrayList<>();

    public void addPayment(DebtPayment payment) {
        payments.add(payment);
        payment.setDebt(this);
    }

    public void removePayment(DebtPayment payment) {
        payments.remove(payment);
        payment.setDebt(null);
    }
}
