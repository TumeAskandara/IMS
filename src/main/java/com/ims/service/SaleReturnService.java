package com.ims.service;

import com.ims.dto.returns.*;
import com.ims.entity.*;
import com.ims.enums.*;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SaleReturnService {

    private final SaleReturnRepository returnRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final BranchInventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final DebtRepository debtRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final MessageService messageService;

    public SaleReturnDTO createReturn(SaleReturnRequest request, Long userId) {
        log.info("Creating return for sale ID: {}", request.getSaleId());

        Sale sale = saleRepository.findById(request.getSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));

        // Reject returns on fully-returned or cancelled sales
        if (sale.isFullyReturned()) {
            throw new IllegalStateException("Sale has already been fully returned");
        }
        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new IllegalStateException("Cannot create return for a cancelled sale");
        }
        if (sale.getStatus() == SaleStatus.REFUNDED) {
            throw new IllegalStateException("Sale has already been fully refunded");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Load pending/approved returns to prevent concurrent double returns
        List<SaleReturn> pendingReturns = returnRepository.findPendingOrApprovedBySaleId(sale.getId());
        Map<Long, Integer> pendingReturnQuantities = new HashMap<>();
        for (SaleReturn pending : pendingReturns) {
            for (SaleReturnItem item : pending.getItems()) {
                pendingReturnQuantities.merge(
                        item.getSaleItem().getId(),
                        item.getQuantityReturned(),
                        Integer::sum
                );
            }
        }

        String returnNumber = generateReturnNumber();
        BigDecimal totalAmount = BigDecimal.ZERO;

        SaleReturn saleReturn = new SaleReturn();
        saleReturn.setReturnNumber(returnNumber);
        saleReturn.setSale(sale);
        saleReturn.setBranch(sale.getBranch());
        saleReturn.setReturnDate(LocalDateTime.now());
        saleReturn.setReturnReason(request.getReturnReason());
        saleReturn.setRefundMethod(RefundMethod.valueOf(request.getRefundMethod()));
        saleReturn.setStatus(ReturnStatus.PENDING);
        saleReturn.setNotes(request.getNotes());
        saleReturn.setCreatedBy(user.getUsername());
        saleReturn.setIsDeleted(false);

        for (SaleReturnRequest.ReturnItemRequest itemRequest : request.getItems()) {
            SaleItem saleItem = saleItemRepository.findById(itemRequest.getSaleItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sale item not found"));

            // Check returnable quantity (already processed returns)
            int returnable = saleItem.getReturnableQuantity();

            // Also account for quantities in pending/approved returns not yet processed
            int pendingQty = pendingReturnQuantities.getOrDefault(saleItem.getId(), 0);
            int effectiveReturnable = returnable - pendingQty;

            if (itemRequest.getQuantity() > effectiveReturnable) {
                throw new IllegalArgumentException(
                        "Return quantity (" + itemRequest.getQuantity() +
                        ") exceeds returnable quantity (" + effectiveReturnable +
                        ") for product " + saleItem.getProduct().getName() +
                        " (already returned: " + saleItem.getQuantityReturned() +
                        ", pending returns: " + pendingQty + ")");
            }

            BigDecimal itemSubtotal = saleItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            SaleReturnItem returnItem = new SaleReturnItem();
            returnItem.setSaleReturn(saleReturn);
            returnItem.setSaleItem(saleItem);
            returnItem.setProduct(saleItem.getProduct());
            returnItem.setQuantityReturned(itemRequest.getQuantity());
            returnItem.setUnitPrice(saleItem.getUnitPrice());
            returnItem.setSubtotal(itemSubtotal);
            returnItem.setReturnReason(itemRequest.getReturnReason());
            returnItem.setCondition(ItemCondition.valueOf(itemRequest.getCondition()));
            returnItem.setRestocked(false);
            returnItem.setCreatedBy(user.getUsername());
            returnItem.setIsDeleted(false);

            saleReturn.addItem(returnItem);
            totalAmount = totalAmount.add(itemSubtotal);
        }

        saleReturn.setTotalAmount(totalAmount);
        saleReturn.setRefundAmount(totalAmount);

        SaleReturn saved = returnRepository.save(saleReturn);
        log.info("Return created successfully: {}", saved.getReturnNumber());

        // Notify managers and admins about new return request
        notificationService.createNotificationForAllAdmins(
                NotificationType.USER_ACTION,
                NotificationPriority.HIGH,
                "New Return Request",
                String.format("Return %s created for sale #%s — %d item(s), total: $%.2f. Awaiting approval.",
                        saved.getReturnNumber(),
                        sale.getInvoiceNumber(),
                        saved.getItems().size(),
                        totalAmount)
        );

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public SaleReturnDTO getReturnById(Long id) {
        SaleReturn saleReturn = returnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found"));
        return mapToDTO(saleReturn);
    }

    @Transactional(readOnly = true)
    public SaleReturnDTO getReturnByNumber(String returnNumber) {
        SaleReturn saleReturn = returnRepository.findByReturnNumber(returnNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found"));
        return mapToDTO(saleReturn);
    }

    @Transactional(readOnly = true)
    public Page<SaleReturnDTO> getAllReturns(Pageable pageable) {
        return returnRepository.findAll(pageable).map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<SaleReturnDTO> getReturnsByBranch(Long branchId, Pageable pageable) {
        return returnRepository.findByBranchIdOrderByCreatedAtDesc(branchId, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<SaleReturnDTO> getReturnsByStatus(ReturnStatus status, Pageable pageable) {
        return returnRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(this::mapToDTO);
    }

    public SaleReturnDTO approveReturn(Long returnId, Long userId) {
        log.info("Approving and processing return: {}", returnId);

        SaleReturn saleReturn = returnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found"));

        if (!saleReturn.isPending()) {
            throw new IllegalStateException("Only pending returns can be approved");
        }

        User approver = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        saleReturn.approve(approver);

        // --- Process refund immediately on approval ---
        Sale sale = saleReturn.getSale();
        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        int totalItemsRestocked = 0;

        for (SaleReturnItem item : saleReturn.getItems()) {
            // Calculate condition-based refund percentage
            BigDecimal refundPercentage = getRefundPercentage(item.getCondition());
            BigDecimal itemRefund = item.getSubtotal()
                    .multiply(refundPercentage)
                    .setScale(2, RoundingMode.HALF_UP);
            item.setRefundPercentage(refundPercentage.multiply(BigDecimal.valueOf(100)));
            item.setRefundAmount(itemRefund);
            totalRefundAmount = totalRefundAmount.add(itemRefund);

            // Update SaleItem.quantityReturned
            SaleItem saleItem = item.getSaleItem();
            saleItem.addReturnedQuantity(item.getQuantityReturned());
            saleItemRepository.save(saleItem);

            // Acquire lock and capture inventory state BEFORE restocking
            BranchInventory inventory = inventoryRepository
                    .findByBranchAndProductForUpdate(saleReturn.getBranch(), item.getProduct())
                    .orElse(null);
            int quantityBefore = inventory != null ? inventory.getQuantityOnHand() : 0;

            // Restock inventory (only for UNOPENED, USED, DEFECTIVE)
            if (canRestock(item.getCondition()) && !item.getRestocked()) {
                if (inventory == null) {
                    throw new ResourceNotFoundException(
                            "Inventory not found for product in branch");
                }
                restockInventory(inventory, item);
                item.markAsRestocked();
                totalItemsRestocked++;
            }

            int quantityAfter = item.getRestocked() ?
                    quantityBefore + item.getQuantityReturned() : quantityBefore;

            // Record stock movement
            recordReturnStockMovement(item, saleReturn.getBranch(), saleReturn.getId(),
                    quantityBefore, quantityAfter);
        }

        // Update refund amount based on condition percentages
        saleReturn.setRefundAmount(totalRefundAmount);

        // Adjust sale revenue
        sale.applyReturn(totalRefundAmount);
        saleRepository.save(sale);

        // Handle debt/credit reduction for credit sales
        if (sale.getPaymentMethod() == PaymentMethod.CREDIT) {
            reduceDebtForReturn(sale, totalRefundAmount);
        }

        // Mark return as completed
        saleReturn.complete();
        SaleReturn saved = returnRepository.save(saleReturn);

        // Notify about completed return
        notificationService.createNotificationForAllAdmins(
                NotificationType.USER_ACTION,
                NotificationPriority.MEDIUM,
                "Return Processed",
                String.format("Return %s for sale #%s approved and processed. Refund: $%.2f, %d item(s) restocked to %s.",
                        saved.getReturnNumber(),
                        sale.getInvoiceNumber(),
                        totalRefundAmount,
                        totalItemsRestocked,
                        saleReturn.getBranch().getName())
        );

        log.info("Return approved and processed: {} (refund: {}, restocked: {} items)",
                saved.getReturnNumber(), totalRefundAmount, totalItemsRestocked);
        return mapToDTO(saved);
    }

    public SaleReturnDTO rejectReturn(Long returnId, Long userId, String reason) {
        log.info("Rejecting return: {}", returnId);

        SaleReturn saleReturn = returnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found"));

        if (!saleReturn.isPending()) {
            throw new IllegalStateException("Only pending returns can be rejected");
        }

        User rejector = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        saleReturn.reject(rejector);
        saleReturn.setNotes((saleReturn.getNotes() != null ? saleReturn.getNotes() + "\n" : "")
                + "Rejection reason: " + reason);

        SaleReturn saved = returnRepository.save(saleReturn);

        // Notify about rejected return
        notificationService.createNotificationForAllAdmins(
                NotificationType.USER_ACTION,
                NotificationPriority.MEDIUM,
                "Return Rejected",
                String.format("Return %s for sale #%s was rejected. Reason: %s",
                        saved.getReturnNumber(),
                        saleReturn.getSale().getInvoiceNumber(),
                        reason)
        );

        log.info("Return rejected: {}", saved.getReturnNumber());
        return mapToDTO(saved);
    }

    /**
     * Manual refund processing — only for APPROVED returns that haven't been completed yet.
     * Normally, approveReturn() auto-processes everything. This is a fallback.
     */
    public SaleReturnDTO processRefund(Long returnId) {
        log.info("Processing refund for return: {}", returnId);

        SaleReturn saleReturn = returnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found"));

        if (saleReturn.isCompleted()) {
            throw new IllegalStateException("Return has already been processed");
        }
        if (!saleReturn.canBeProcessed()) {
            throw new IllegalStateException("Return cannot be processed in current status");
        }

        Sale sale = saleReturn.getSale();
        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        int totalItemsRestocked = 0;

        for (SaleReturnItem item : saleReturn.getItems()) {
            // Calculate condition-based refund percentage
            BigDecimal refundPercentage = getRefundPercentage(item.getCondition());
            BigDecimal itemRefund = item.getSubtotal()
                    .multiply(refundPercentage)
                    .setScale(2, RoundingMode.HALF_UP);
            item.setRefundPercentage(refundPercentage.multiply(BigDecimal.valueOf(100)));
            item.setRefundAmount(itemRefund);
            totalRefundAmount = totalRefundAmount.add(itemRefund);

            // Update SaleItem.quantityReturned
            SaleItem saleItem = item.getSaleItem();
            saleItem.addReturnedQuantity(item.getQuantityReturned());
            saleItemRepository.save(saleItem);

            // Acquire lock and capture inventory state BEFORE restocking
            BranchInventory inventory = inventoryRepository
                    .findByBranchAndProductForUpdate(saleReturn.getBranch(), item.getProduct())
                    .orElse(null);
            int quantityBefore = inventory != null ? inventory.getQuantityOnHand() : 0;

            // Restock inventory (only for UNOPENED, USED, DEFECTIVE)
            if (canRestock(item.getCondition()) && !item.getRestocked()) {
                if (inventory == null) {
                    throw new ResourceNotFoundException(
                            "Inventory not found for product in branch");
                }
                restockInventory(inventory, item);
                item.markAsRestocked();
                totalItemsRestocked++;
            }

            int quantityAfter = item.getRestocked() ?
                    quantityBefore + item.getQuantityReturned() : quantityBefore;

            // Record stock movement
            recordReturnStockMovement(item, saleReturn.getBranch(), saleReturn.getId(),
                    quantityBefore, quantityAfter);
        }

        // Update refund amount based on condition percentages
        saleReturn.setRefundAmount(totalRefundAmount);

        // Adjust sale revenue
        sale.applyReturn(totalRefundAmount);
        saleRepository.save(sale);

        // Handle debt/credit reduction for credit sales
        if (sale.getPaymentMethod() == PaymentMethod.CREDIT) {
            reduceDebtForReturn(sale, totalRefundAmount);
        }

        // Mark return as completed
        saleReturn.complete();
        SaleReturn saved = returnRepository.save(saleReturn);

        // Notify about completed return
        notificationService.createNotificationForAllAdmins(
                NotificationType.USER_ACTION,
                NotificationPriority.MEDIUM,
                "Return Processed",
                String.format("Return %s for sale #%s processed. Refund: $%.2f, %d item(s) restocked.",
                        saved.getReturnNumber(),
                        sale.getInvoiceNumber(),
                        totalRefundAmount,
                        totalItemsRestocked)
        );

        log.info("Refund processed successfully: {} (refund: {}, restocked: {} items)",
                saved.getReturnNumber(), totalRefundAmount, totalItemsRestocked);
        return mapToDTO(saved);
    }

    private BigDecimal getRefundPercentage(ItemCondition condition) {
        return switch (condition) {
            case UNOPENED, DEFECTIVE -> BigDecimal.ONE;
            case USED -> new BigDecimal("0.80");
            case DAMAGED -> new BigDecimal("0.50");
        };
    }

    private boolean canRestock(ItemCondition condition) {
        return condition == ItemCondition.UNOPENED ||
               condition == ItemCondition.USED ||
               condition == ItemCondition.DEFECTIVE;
    }

    private void restockInventory(BranchInventory inventory, SaleReturnItem returnItem) {
        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + returnItem.getQuantityReturned());
        inventory.setQuantityAvailable(inventory.getQuantityOnHand() - inventory.getQuantityReserved());
        inventoryRepository.save(inventory);

        log.info("Restocked {} units of product {}",
                returnItem.getQuantityReturned(),
                returnItem.getProduct().getName());
    }

    private void recordReturnStockMovement(SaleReturnItem returnItem, Branch branch, Long returnId,
                                             int quantityBefore, int quantityAfter) {
        StockMovement movement = StockMovement.builder()
                .product(returnItem.getProduct())
                .branch(branch)
                .movementType(StockMovementType.RETURN)
                .quantity(returnItem.getQuantityReturned())
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .referenceType("SALE_RETURN")
                .referenceId(returnId)
                .notes("Return - Condition: " + returnItem.getCondition().name())
                .build();
        stockMovementRepository.save(movement);
    }

    private void reduceDebtForReturn(Sale sale, BigDecimal refundAmount) {
        debtRepository.findBySaleId(sale.getId()).ifPresent(debt -> {
            // Reduce balance due
            BigDecimal newBalance = debt.getBalanceDue().subtract(refundAmount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                newBalance = BigDecimal.ZERO;
            }
            debt.setBalanceDue(newBalance);

            // If debt is fully paid after reduction, update status
            if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
                debt.setStatus(DebtStatus.FULLY_PAID);
            }
            debtRepository.save(debt);

            // Update CreditAccount.totalCreditUsed
            CreditAccount creditAccount = debt.getCreditAccount();
            if (creditAccount != null) {
                BigDecimal newCreditUsed = creditAccount.getTotalCreditUsed().subtract(refundAmount);
                if (newCreditUsed.compareTo(BigDecimal.ZERO) < 0) {
                    newCreditUsed = BigDecimal.ZERO;
                }
                creditAccount.setTotalCreditUsed(newCreditUsed);
                creditAccountRepository.save(creditAccount);
            }

            // Update Customer.currentDebt if customer is linked
            Customer customer = sale.getCustomer();
            if (customer != null) {
                customer.reduceDebt(refundAmount);
                customerRepository.save(customer);
            }

            log.info("Reduced debt for sale {} by {}. New balance: {}",
                    sale.getInvoiceNumber(), refundAmount, newBalance);
        });
    }

    private String generateReturnNumber() {
        long count = returnRepository.count() + 1;
        return String.format("RTN-%05d", count);
    }

    private SaleReturnDTO mapToDTO(SaleReturn saleReturn) {
        return SaleReturnDTO.builder()
                .id(saleReturn.getId())
                .returnNumber(saleReturn.getReturnNumber())
                .originalInvoiceNumber(saleReturn.getSale().getInvoiceNumber())
                .saleId(saleReturn.getSale().getId())
                .customerName(saleReturn.getSale().getCustomerName())
                .returnReason(saleReturn.getReturnReason())
                .refundMethod(saleReturn.getRefundMethod().name())
                .status(saleReturn.getStatus().name())
                .totalAmount(saleReturn.getTotalAmount())
                .refundAmount(saleReturn.getRefundAmount())
                .processedBy(saleReturn.getProcessedBy() != null ?
                        saleReturn.getProcessedBy().getFullName() : null)
                .branchName(saleReturn.getBranch().getName())
                .items(saleReturn.getItems().stream()
                        .map(this::mapItemToDTO)
                        .collect(Collectors.toList()))
                .notes(saleReturn.getNotes())
                .returnDate(saleReturn.getReturnDate())
                .processedDate(saleReturn.getProcessedDate())
                .createdAt(saleReturn.getCreatedAt())
                .build();
    }

    private SaleReturnItemDTO mapItemToDTO(SaleReturnItem item) {
        return SaleReturnItemDTO.builder()
                .id(item.getId())
                .productName(item.getProduct().getName())
                .productSku(item.getProduct().getSku())
                .productId(item.getProduct().getId())
                .quantityReturned(item.getQuantityReturned())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .refundPercentage(item.getRefundPercentage())
                .refundAmount(item.getRefundAmount())
                .returnReason(item.getReturnReason())
                .condition(item.getCondition().name())
                .build();
    }
}
