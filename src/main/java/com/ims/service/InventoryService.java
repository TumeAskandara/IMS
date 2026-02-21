package com.ims.service;

import com.ims.entity.*;
import com.ims.enums.NotificationPriority;
import com.ims.enums.NotificationType;
import com.ims.enums.Role;
import com.ims.enums.StockMovementType;
import com.ims.exception.BadRequestException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.BranchInventoryRepository;
import com.ims.repository.BranchRepository;
import com.ims.repository.ProductRepository;
import com.ims.repository.StockMovementRepository;
import com.ims.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final BranchInventoryRepository branchInventoryRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<BranchInventory> getBranchInventory(Long branchId, Pageable pageable) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch", "id", branchId);
        }
        return branchInventoryRepository.findByBranchId(branchId, pageable);
    }

    @Transactional(readOnly = true)
    public BranchInventory getProductStock(Long branchId, Long productId) {
        return branchInventoryRepository.findByBranchIdAndProductId(branchId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory record not found for branch " + branchId + " and product " + productId));
    }

    @Transactional
    public BranchInventory adjustStock(Long branchId, Long productId, Integer quantity,
                                       StockMovementType movementType, String notes) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", branchId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        BranchInventory inventory = branchInventoryRepository
                .findByBranchIdAndProductId(branchId, productId)
                .orElse(BranchInventory.builder()
                        .branch(branch)
                        .product(product)
                        .quantityOnHand(0)
                        .quantityReserved(0)
                        .quantityAvailable(0)
                        .build());

        int oldQuantity = inventory.getQuantityOnHand();
        int newQuantity = oldQuantity + quantity;

        if (newQuantity < 0) {
            throw new BadRequestException("Insufficient stock. Available: " + oldQuantity);
        }

        inventory.setQuantityOnHand(newQuantity);
        inventory.setQuantityAvailable(newQuantity - inventory.getQuantityReserved());

        BranchInventory saved = branchInventoryRepository.save(inventory);

        // Create stock movement record
        StockMovement movement = StockMovement.builder()
                .product(product)
                .branch(branch)
                .movementType(movementType)
                .quantity(Math.abs(quantity))
                .quantityBefore(oldQuantity)
                .quantityAfter(newQuantity)
                .notes(notes)
                .build();
        stockMovementRepository.save(movement);

        // Check stock levels and send notifications
        checkStockLevels(saved, product, branch);

        return saved;
    }

    // Check stock levels and notify managers
    private void checkStockLevels(BranchInventory inventory, Product product, Branch branch) {
        // Check if low stock
        if (product.getReorderLevel() != null &&
                inventory.getQuantityAvailable() < product.getReorderLevel() &&
                inventory.getQuantityAvailable() > 0) {

            // Find all managers in this branch
            List<User> managers = userRepository.findByBranchAndRole(branch, Role.MANAGER);

            // Create notification for each manager
            for (User manager : managers) {
                notificationService.createNotification(
                        manager.getId(),
                        NotificationType.LOW_STOCK,
                        NotificationPriority.HIGH,
                        "Low Stock Alert",
                        String.format("Product '%s' is low. Current: %d, Reorder Level: %d",
                                product.getName(),
                                inventory.getQuantityAvailable(),
                                product.getReorderLevel())
                );
            }
        }

        // Check if out of stock
        if (inventory.getQuantityAvailable() == 0) {
            List<User> managers = userRepository.findByBranchAndRole(branch, Role.MANAGER);

            for (User manager : managers) {
                notificationService.createNotification(
                        manager.getId(),
                        NotificationType.OUT_OF_STOCK,
                        NotificationPriority.CRITICAL,
                        "Out of Stock!",
                        String.format("Product '%s' is OUT OF STOCK!", product.getName())
                );
            }
        }
    }

    @Transactional(readOnly = true)
    public List<BranchInventory> getLowStockItems(Long branchId) {
        return branchInventoryRepository.findLowStockItems(branchId);
    }

    @Transactional(readOnly = true)
    public Page<StockMovement> getStockMovements(Long branchId, Pageable pageable) {
        return stockMovementRepository.findByBranchIdOrderByCreatedAtDesc(branchId, pageable);
    }

    @Transactional
    public void initializeInventory(Long branchId, Long productId, Integer initialQuantity) {
        adjustStock(branchId, productId, initialQuantity, StockMovementType.PURCHASE, "Initial stock");
    }

    @Transactional(readOnly = true)
    public Page<BranchInventory> searchInventory(Long branchId, String searchTerm, Pageable pageable) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch", "id", branchId);
        }
        return branchInventoryRepository.searchInventory(branchId, searchTerm, pageable);
    }

    @Transactional(readOnly = true)
    public Page<BranchInventory> getInventoryByCategory(Long branchId, Long categoryId, Pageable pageable) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch", "id", branchId);
        }
        return branchInventoryRepository.findByBranchIdAndCategoryId(branchId, categoryId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<BranchInventory> getInStockItems(Long branchId, Pageable pageable) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch", "id", branchId);
        }
        return branchInventoryRepository.findInStockItems(branchId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<BranchInventory> getOutOfStockItems(Long branchId, Pageable pageable) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch", "id", branchId);
        }
        return branchInventoryRepository.findOutOfStockItems(branchId, pageable);
    }
}