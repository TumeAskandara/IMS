package com.ims.entity;

import com.ims.enums.RefundMethod;
import com.ims.enums.ReturnStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sale_returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String returnNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(nullable = false)
    private LocalDateTime returnDate;

    @Column(nullable = false, length = 500)
    private String returnReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundMethod refundMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReturnStatus status = ReturnStatus.PENDING;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column
    private LocalDateTime processedDate;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleReturnItem> items = new ArrayList<>();

    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    // Business methods
    public void addItem(SaleReturnItem item) {
        items.add(item);
        item.setSaleReturn(this);
    }

    public void approve(User approver) {
        this.status = ReturnStatus.APPROVED;
        this.processedBy = approver;
        this.processedDate = LocalDateTime.now();
    }

    public void reject(User rejector) {
        this.status = ReturnStatus.REJECTED;
        this.processedBy = rejector;
        this.processedDate = LocalDateTime.now();
    }

    public void complete() {
        this.status = ReturnStatus.COMPLETED;
        this.processedDate = LocalDateTime.now();
    }

    public boolean canBeProcessed() {
        return status == ReturnStatus.PENDING || status == ReturnStatus.APPROVED;
    }

    public boolean isPending() {
        return status == ReturnStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == ReturnStatus.COMPLETED;
    }
}