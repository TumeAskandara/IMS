package com.ims.entity;

import com.ims.enums.ExpenseCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    private User recordedBy;

    // ONLY 4 REQUIRED FIELDS FROM USER
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category; // 1. What type of expense

    @Column(nullable = false, length = 200)
    private String description; // 2. What was it for

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount; // 3. How much

    @Column(nullable = false)
    private LocalDate expenseDate; // 4. When

    // Optional field
    @Column(length = 1000)
    private String notes;

    // Auto-filled fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;
}