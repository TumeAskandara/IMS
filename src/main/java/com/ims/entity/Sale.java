package com.ims.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ims.enums.PaymentMethod;
import com.ims.enums.SaleStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "sales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "sale_date", nullable = false)
    private LocalDateTime saleDate;

    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "returned_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal returnedAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "amount_paid", precision = 12, scale = 2, nullable = false)
    private BigDecimal amountPaid;

    @Column(name = "amount_due", precision = 12, scale = 2)
    private BigDecimal amountDue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference  // THIS BREAKS THE CIRCULAR REFERENCE
    @Builder.Default
    private List<SaleItem> saleItems = new ArrayList<>();

    @OneToMany(mappedBy = "sale")
    @JsonIgnore
    @Builder.Default
    private List<SaleReturn> saleReturns = new ArrayList<>();

    public void addSaleItem(SaleItem item) {
        saleItems.add(item);
        item.setSale(this);
    }

    public void removeSaleItem(SaleItem item) {
        saleItems.remove(item);
        item.setSale(null);
    }

    @PrePersist
    @PreUpdate
    private void calculateNetAmount() {
        if (totalAmount != null) {
            if (returnedAmount == null) {
                returnedAmount = BigDecimal.ZERO;
            }
            this.netAmount = totalAmount.subtract(returnedAmount);
        }
    }

    public void applyReturn(BigDecimal refundAmount) {
        if (returnedAmount == null) {
            returnedAmount = BigDecimal.ZERO;
        }
        this.returnedAmount = this.returnedAmount.add(refundAmount);
        this.netAmount = this.totalAmount.subtract(this.returnedAmount);

        if (this.returnedAmount.compareTo(this.totalAmount) >= 0) {
            this.status = SaleStatus.REFUNDED;
        } else if (this.returnedAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = SaleStatus.PARTIALLY_RETURNED;
        }
    }

    public boolean isFullyReturned() {
        return returnedAmount != null && returnedAmount.compareTo(totalAmount) >= 0;
    }
}
