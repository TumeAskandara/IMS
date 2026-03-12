package com.ims.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stock_take_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTakeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_take_id", nullable = false)
    private StockTake stockTake;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "system_quantity", nullable = false)
    private Integer systemQuantity;

    @Column(name = "physical_quantity")
    private Integer physicalQuantity;

    @Column(name = "discrepancy")
    private Integer discrepancy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public void calculateDiscrepancy() {
        if (physicalQuantity != null) {
            this.discrepancy = physicalQuantity - systemQuantity;
        }
    }
}