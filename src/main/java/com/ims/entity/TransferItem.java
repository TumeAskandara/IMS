package com.ims.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transfer_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", nullable = false)
    @JsonBackReference  // THIS BREAKS THE CIRCULAR REFERENCE
    private StockTransfer stockTransfer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_requested", nullable = false)
    private Integer quantityRequested;

    @Column(name = "quantity_shipped")
    private Integer quantityShipped = 0;

    @Column(name = "quantity_received")
    private Integer quantityReceived = 0;

    @Column(name = "quantity_damaged")
    private Integer quantityDamaged = 0;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
