package com.ims.service;

import com.ims.dto.purchaseorder.*;
import com.ims.entity.*;
import com.ims.enums.*;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.*;
import com.ims.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final SupplierRepository supplierRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    public PurchaseOrderDTO createPurchaseOrder(PurchaseOrderRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        User currentUser = securityUtils.getCurrentUser();

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(generatePoNumber())
                .supplier(supplier)
                .branch(branch)
                .orderedBy(currentUser)
                .orderDate(LocalDateTime.now())
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .status(PurchaseOrderStatus.DRAFT)
                .taxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO)
                .shippingCost(request.getShippingCost() != null ? request.getShippingCost() : BigDecimal.ZERO)
                .notes(request.getNotes())
                .build();

        for (PurchaseOrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemReq.getProductId()));

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .product(product)
                    .quantityOrdered(itemReq.getQuantityOrdered())
                    .unitCost(itemReq.getUnitCost())
                    .lineTotal(itemReq.getUnitCost().multiply(BigDecimal.valueOf(itemReq.getQuantityOrdered())))
                    .build();
            po.addItem(item);
        }

        po.recalculateTotals();
        po = purchaseOrderRepository.save(po);
        log.info("Created purchase order: {}", po.getPoNumber());
        return mapToDTO(po);
    }

    public PurchaseOrderDTO submitPurchaseOrder(Long id) {
        PurchaseOrder po = findByIdWithItems(id);
        validateStatusTransition(po, PurchaseOrderStatus.DRAFT, "submit");

        po.setStatus(PurchaseOrderStatus.SUBMITTED);
        po = purchaseOrderRepository.save(po);

        notificationService.createNotificationForAllAdmins(
                NotificationType.PURCHASE_ORDER_SUBMITTED,
                NotificationPriority.MEDIUM,
                "Purchase Order Submitted",
                "PO " + po.getPoNumber() + " submitted by " + po.getOrderedBy().getFullName()
                        + " for " + po.getSupplier().getName() + " — Total: " + po.getTotalAmount()
        );

        log.info("Submitted PO: {}", po.getPoNumber());
        return mapToDTO(po);
    }

    public PurchaseOrderDTO approvePurchaseOrder(Long id) {
        PurchaseOrder po = findByIdWithItems(id);
        validateStatusTransition(po, PurchaseOrderStatus.SUBMITTED, "approve");

        User approver = securityUtils.getCurrentUser();
        po.setStatus(PurchaseOrderStatus.APPROVED);
        po.setApprovedBy(approver);
        po = purchaseOrderRepository.save(po);

        notificationService.createNotification(
                po.getOrderedBy().getId(),
                NotificationType.PURCHASE_ORDER_APPROVED,
                NotificationPriority.MEDIUM,
                "Purchase Order Approved",
                "PO " + po.getPoNumber() + " has been approved by " + approver.getFullName()
        );

        log.info("Approved PO: {} by {}", po.getPoNumber(), approver.getFullName());
        return mapToDTO(po);
    }

    public PurchaseOrderDTO receiveGoods(Long id, GoodsReceiptRequest request) {
        PurchaseOrder po = findByIdWithItems(id);
        if (po.getStatus() != PurchaseOrderStatus.APPROVED
                && po.getStatus() != PurchaseOrderStatus.SHIPPED
                && po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalStateException(
                    "Cannot receive goods for PO in status: " + po.getStatus());
        }

        for (GoodsReceiptItemRequest receiptItem : request.getItems()) {
            PurchaseOrderItem poItem = po.getItems().stream()
                    .filter(i -> i.getId().equals(receiptItem.getPoItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "PO item not found: " + receiptItem.getPoItemId()));

            int qtyReceived = receiptItem.getQuantityReceived();
            int qtyDamaged = receiptItem.getQuantityDamaged() != null ? receiptItem.getQuantityDamaged() : 0;
            int totalNewReceived = poItem.getQuantityReceived() + qtyReceived;

            if (totalNewReceived > poItem.getQuantityOrdered()) {
                throw new IllegalArgumentException(
                        "Total received (" + totalNewReceived + ") exceeds ordered (" +
                                poItem.getQuantityOrdered() + ") for product: " + poItem.getProduct().getName());
            }

            poItem.setQuantityReceived(totalNewReceived);
            poItem.setQuantityDamaged(poItem.getQuantityDamaged() + qtyDamaged);

            // Update inventory: only add good (non-damaged) items
            int goodQty = qtyReceived - qtyDamaged;
            if (goodQty > 0) {
                BranchInventory inventory = branchInventoryRepository
                        .findByBranchIdAndProductIdForUpdate(po.getBranch().getId(), poItem.getProduct().getId())
                        .orElse(null);

                int qtyBefore;
                if (inventory == null) {
                    inventory = BranchInventory.builder()
                            .branch(po.getBranch())
                            .product(poItem.getProduct())
                            .quantityOnHand(0)
                            .quantityReserved(0)
                            .quantityAvailable(0)
                            .build();
                    qtyBefore = 0;
                } else {
                    qtyBefore = inventory.getQuantityOnHand();
                }

                inventory.setQuantityOnHand(inventory.getQuantityOnHand() + goodQty);
                inventory.setQuantityAvailable(inventory.getQuantityOnHand() - inventory.getQuantityReserved());
                inventory.setLastRestockDate(LocalDateTime.now());
                branchInventoryRepository.save(inventory);

                // Create stock movement
                StockMovement movement = StockMovement.builder()
                        .product(poItem.getProduct())
                        .branch(po.getBranch())
                        .movementType(StockMovementType.PURCHASE)
                        .quantity(goodQty)
                        .quantityBefore(qtyBefore)
                        .quantityAfter(inventory.getQuantityOnHand())
                        .referenceType("PURCHASE_ORDER")
                        .referenceId(po.getId())
                        .notes("Received from PO " + po.getPoNumber())
                        .build();
                stockMovementRepository.save(movement);
            }
        }

        // Determine PO status
        boolean allReceived = po.getItems().stream()
                .allMatch(i -> i.getQuantityReceived().equals(i.getQuantityOrdered()));
        boolean anyReceived = po.getItems().stream()
                .anyMatch(i -> i.getQuantityReceived() > 0);

        if (allReceived) {
            po.setStatus(PurchaseOrderStatus.RECEIVED);
            po.setActualDeliveryDate(LocalDate.now());
        } else if (anyReceived) {
            po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        po = purchaseOrderRepository.save(po);

        notificationService.createNotificationForAllAdmins(
                NotificationType.PURCHASE_ORDER_RECEIVED,
                NotificationPriority.MEDIUM,
                "Goods Received",
                "Goods received for PO " + po.getPoNumber() + " — Status: " + po.getStatus()
        );

        log.info("Received goods for PO: {} — Status: {}", po.getPoNumber(), po.getStatus());
        return mapToDTO(po);
    }

    public PurchaseOrderDTO cancelPurchaseOrder(Long id, String reason) {
        PurchaseOrder po = findByIdWithItems(id);
        if (po.getStatus() == PurchaseOrderStatus.RECEIVED
                || po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel PO in status: " + po.getStatus());
        }

        po.setStatus(PurchaseOrderStatus.CANCELLED);
        po.setCancellationReason(reason);
        po = purchaseOrderRepository.save(po);

        notificationService.createNotification(
                po.getOrderedBy().getId(),
                NotificationType.PURCHASE_ORDER_CANCELLED,
                NotificationPriority.HIGH,
                "Purchase Order Cancelled",
                "PO " + po.getPoNumber() + " has been cancelled. Reason: " + reason
        );

        log.info("Cancelled PO: {} — Reason: {}", po.getPoNumber(), reason);
        return mapToDTO(po);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDTO getPurchaseOrderById(Long id) {
        return mapToDTO(findByIdWithItems(id));
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrderDTO> getAllPurchaseOrders(Pageable pageable) {
        return purchaseOrderRepository.findAll(pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrderDTO> getPurchaseOrdersByBranch(Long branchId, Pageable pageable) {
        return purchaseOrderRepository.findByBranchId(branchId, pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrderDTO> getPurchaseOrdersByStatus(PurchaseOrderStatus status, Pageable pageable) {
        return purchaseOrderRepository.findByStatus(status, pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrderDTO> getPurchaseOrdersBySupplier(Long supplierId, Pageable pageable) {
        return purchaseOrderRepository.findBySupplierId(supplierId, pageable).map(this::mapToDTO);
    }

    private PurchaseOrder findByIdWithItems(Long id) {
        return purchaseOrderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + id));
    }

    private void validateStatusTransition(PurchaseOrder po, PurchaseOrderStatus expectedCurrent, String action) {
        if (po.getStatus() != expectedCurrent) {
            throw new IllegalStateException(
                    "Cannot " + action + " PO in status: " + po.getStatus() + ". Expected: " + expectedCurrent);
        }
    }

    private String generatePoNumber() {
        long count = purchaseOrderRepository.count() + 1;
        return String.format("PO-%06d", count);
    }

    private PurchaseOrderDTO mapToDTO(PurchaseOrder po) {
        List<PurchaseOrderItemDTO> itemDTOs = po.getItems() != null
                ? po.getItems().stream().map(this::mapItemToDTO).toList()
                : List.of();

        return PurchaseOrderDTO.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplier().getId())
                .supplierName(po.getSupplier().getName())
                .branchId(po.getBranch().getId())
                .branchName(po.getBranch().getName())
                .orderedByName(po.getOrderedBy().getFullName())
                .approvedByName(po.getApprovedBy() != null ? po.getApprovedBy().getFullName() : null)
                .orderDate(po.getOrderDate())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .actualDeliveryDate(po.getActualDeliveryDate())
                .status(po.getStatus().name())
                .subtotal(po.getSubtotal())
                .taxAmount(po.getTaxAmount())
                .shippingCost(po.getShippingCost())
                .totalAmount(po.getTotalAmount())
                .notes(po.getNotes())
                .cancellationReason(po.getCancellationReason())
                .items(itemDTOs)
                .createdAt(po.getCreatedAt())
                .build();
    }

    private PurchaseOrderItemDTO mapItemToDTO(PurchaseOrderItem item) {
        return PurchaseOrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productSku(item.getProduct().getSku())
                .quantityOrdered(item.getQuantityOrdered())
                .quantityReceived(item.getQuantityReceived())
                .quantityDamaged(item.getQuantityDamaged())
                .unitCost(item.getUnitCost())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
