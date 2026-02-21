package com.ims.entity;

import com.ims.enums.CustomerStatus;
import com.ims.enums.CustomerType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Added generation strategy
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String customerId; // Auto-generated: CUST-0001

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 50)
    private String taxId; // Tax identification number

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerType customerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus status;

    @Column(precision = 15, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO; // Maximum credit allowed

    @Column(precision = 15, scale = 2)
    private BigDecimal currentDebt = BigDecimal.ZERO; // Current outstanding debt

    @Column(precision = 15, scale = 2)
    private BigDecimal lifetimeValue = BigDecimal.ZERO; // Total purchases to date

    @Column
    private Integer totalPurchases = 0; // Number of purchases made

    @Column
    private LocalDateTime lastPurchaseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch; // Primary branch for this customer

    @Column(length = 1000)
    private String notes;

    // Relationships
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Sale> sales = new ArrayList<>();

    // REMOVED: Direct debts relationship - access debts through CreditAccount instead
    // If you need to get debts for a customer, use the CreditAccount entity

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = CustomerStatus.ACTIVE;
        }
        if (customerType == null) {
            customerType = CustomerType.RETAIL;
        }
    }

    // Business methods
    public void updatePurchaseStats(BigDecimal saleAmount) {
        this.lifetimeValue = this.lifetimeValue.add(saleAmount);
        this.totalPurchases++;
        this.lastPurchaseDate = LocalDateTime.now();
    }

    public BigDecimal getAvailableCredit() {
        return creditLimit.subtract(currentDebt);
    }

    public boolean canPurchaseOnCredit(BigDecimal amount) {
        return status == CustomerStatus.ACTIVE &&
                getAvailableCredit().compareTo(amount) >= 0;
    }

    public void addDebt(BigDecimal amount) {
        this.currentDebt = this.currentDebt.add(amount);
    }

    public void reduceDebt(BigDecimal amount) {
        this.currentDebt = this.currentDebt.subtract(amount);
        if (this.currentDebt.compareTo(BigDecimal.ZERO) < 0) {
            this.currentDebt = BigDecimal.ZERO;
        }
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    public boolean hasOutstandingDebt() {
        return currentDebt.compareTo(BigDecimal.ZERO) > 0;
    }
}