package com.ims.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ims.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransfer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_number", nullable = false, unique = true, length = 50)
    private String transferNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_branch_id", nullable = false)
    private Branch sourceBranch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_branch_id", nullable = false)
    private Branch destinationBranch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "ship_date")
    private LocalDateTime shipDate;

    @Column(name = "receive_date")
    private LocalDateTime receiveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @OneToMany(mappedBy = "stockTransfer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference  // THIS BREAKS THE CIRCULAR REFERENCE
    @Builder.Default
    private List<TransferItem> transferItems = new ArrayList<>();

    public void addTransferItem(TransferItem item) {
        transferItems.add(item);
        item.setStockTransfer(this);
    }

    public void removeTransferItem(TransferItem item) {
        transferItems.remove(item);
        item.setStockTransfer(null);
    }
}
