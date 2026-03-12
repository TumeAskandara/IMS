package com.ims.entity;

import com.ims.enums.StockTakeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_takes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTake extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_take_number", unique = true, nullable = false, length = 30)
    private String stockTakeNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by")
    private User initiatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private User completedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockTakeStatus status;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "total_items")
    private Integer totalItems = 0;

    @Column(name = "discrepancy_count")
    private Integer discrepancyCount = 0;

    @OneToMany(mappedBy = "stockTake", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockTakeItem> items = new ArrayList<>();

    public void addItem(StockTakeItem item) {
        items.add(item);
        item.setStockTake(this);
    }
}