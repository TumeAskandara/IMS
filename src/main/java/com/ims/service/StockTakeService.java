package com.ims.service;

import com.ims.entity.*;
import com.ims.enums.*;
import com.ims.exception.BadRequestException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.*;
import com.ims.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockTakeService {

    private final StockTakeRepository stockTakeRepository;
    private final StockTakeItemRepository stockTakeItemRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final BranchRepository branchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    public StockTake initiateStockTake(Long branchId, String notes) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", branchId));
        User user = securityUtils.getCurrentUser();

        StockTake stockTake = StockTake.builder()
                .stockTakeNumber(generateStockTakeNumber())
                .branch(branch)
                .initiatedBy(user)
                .status(StockTakeStatus.IN_PROGRESS)
                .startDate(LocalDateTime.now())
                .notes(notes)
                .build();

        List<BranchInventory> inventory = branchInventoryRepository.findByBranchId(branchId);
        for (BranchInventory bi : inventory) {
            StockTakeItem item = StockTakeItem.builder()
                    .product(bi.getProduct())
                    .systemQuantity(bi.getQuantityOnHand())
                    .build();
            stockTake.addItem(item);
        }
        stockTake.setTotalItems(inventory.size());

        stockTake = stockTakeRepository.save(stockTake);
        log.info("Initiated stock take {} for branch {}", stockTake.getStockTakeNumber(), branch.getName());
        return stockTake;
    }

    public StockTake updatePhysicalCount(Long stockTakeId, Long itemId, Integer physicalQuantity, String notes) {
        StockTake stockTake = stockTakeRepository.findByIdWithItems(stockTakeId)
                .orElseThrow(() -> new ResourceNotFoundException("StockTake", "id", stockTakeId));

        if (stockTake.getStatus() != StockTakeStatus.IN_PROGRESS) {
            throw new BadRequestException("Stock take is not in progress");
        }

        StockTakeItem item = stockTake.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("StockTakeItem", "id", itemId));

        item.setPhysicalQuantity(physicalQuantity);
        item.setNotes(notes);
        item.calculateDiscrepancy();

        stockTakeItemRepository.save(item);
        return stockTake;
    }

    public StockTake completeStockTake(Long stockTakeId, boolean applyAdjustments) {
        StockTake stockTake = stockTakeRepository.findByIdWithItems(stockTakeId)
                .orElseThrow(() -> new ResourceNotFoundException("StockTake", "id", stockTakeId));

        if (stockTake.getStatus() != StockTakeStatus.IN_PROGRESS) {
            throw new BadRequestException("Stock take is not in progress");
        }

        User user = securityUtils.getCurrentUser();
        int discrepancyCount = 0;

        for (StockTakeItem item : stockTake.getItems()) {
            if (item.getPhysicalQuantity() == null) {
                throw new BadRequestException("All items must have physical count before completing. Missing: " + item.getProduct().getName());
            }
            item.calculateDiscrepancy();
            if (item.getDiscrepancy() != 0) {
                discrepancyCount++;

                if (applyAdjustments) {
                    BranchInventory inventory = branchInventoryRepository
                            .findByBranchIdAndProductIdForUpdate(stockTake.getBranch().getId(), item.getProduct().getId())
                            .orElse(null);

                    if (inventory != null) {
                        int oldQty = inventory.getQuantityOnHand();
                        inventory.setQuantityOnHand(item.getPhysicalQuantity());
                        inventory.setQuantityAvailable(item.getPhysicalQuantity() - inventory.getQuantityReserved());
                        branchInventoryRepository.save(inventory);

                        StockMovement movement = StockMovement.builder()
                                .product(item.getProduct())
                                .branch(stockTake.getBranch())
                                .movementType(StockMovementType.STOCK_TAKE)
                                .quantity(Math.abs(item.getDiscrepancy()))
                                .quantityBefore(oldQty)
                                .quantityAfter(item.getPhysicalQuantity())
                                .referenceType("STOCK_TAKE")
                                .referenceId(stockTake.getId())
                                .notes("Stock take adjustment: " + stockTake.getStockTakeNumber())
                                .build();
                        stockMovementRepository.save(movement);
                    }
                }
            }
        }

        stockTake.setStatus(StockTakeStatus.COMPLETED);
        stockTake.setCompletedBy(user);
        stockTake.setEndDate(LocalDateTime.now());
        stockTake.setDiscrepancyCount(discrepancyCount);

        stockTake = stockTakeRepository.save(stockTake);

        if (discrepancyCount > 0) {
            notificationService.createNotificationForAllAdmins(
                    NotificationType.STOCK_RECONCILIATION,
                    NotificationPriority.HIGH,
                    "Stock Take Completed with Discrepancies",
                    String.format("Stock take %s at %s completed. %d discrepancies found out of %d items.%s",
                            stockTake.getStockTakeNumber(),
                            stockTake.getBranch().getName(),
                            discrepancyCount,
                            stockTake.getTotalItems(),
                            applyAdjustments ? " Adjustments applied." : " Adjustments NOT applied.")
            );
        }

        log.info("Completed stock take {} with {} discrepancies", stockTake.getStockTakeNumber(), discrepancyCount);
        return stockTake;
    }

    public StockTake cancelStockTake(Long stockTakeId) {
        StockTake stockTake = stockTakeRepository.findById(stockTakeId)
                .orElseThrow(() -> new ResourceNotFoundException("StockTake", "id", stockTakeId));

        if (stockTake.getStatus() != StockTakeStatus.IN_PROGRESS) {
            throw new BadRequestException("Only in-progress stock takes can be cancelled");
        }

        stockTake.setStatus(StockTakeStatus.CANCELLED);
        stockTake.setEndDate(LocalDateTime.now());
        return stockTakeRepository.save(stockTake);
    }

    @Transactional(readOnly = true)
    public StockTake getStockTakeById(Long id) {
        return stockTakeRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTake", "id", id));
    }

    @Transactional(readOnly = true)
    public Page<StockTake> getAllStockTakes(Pageable pageable) {
        return stockTakeRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockTake> getStockTakesByBranch(Long branchId, Pageable pageable) {
        return stockTakeRepository.findByBranchIdOrderByCreatedAtDesc(branchId, pageable);
    }

    private String generateStockTakeNumber() {
        long count = stockTakeRepository.count() + 1;
        return String.format("ST-%06d", count);
    }
}