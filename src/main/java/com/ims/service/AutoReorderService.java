package com.ims.service;

import com.ims.entity.*;
import com.ims.enums.*;
import com.ims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoReorderService {

    private final BranchInventoryRepository branchInventoryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 6 * * ?")
    @Transactional
    public void checkAndCreateAutoReorders() {
        log.info("Running auto-reorder check...");

        List<BranchInventory> lowStockItems = branchInventoryRepository.findAllLowStockItems();
        int poCreated = 0;

        for (BranchInventory item : lowStockItems) {
            Product product = item.getProduct();
            Branch branch = item.getBranch();

            if (product.getReorderLevel() == null || product.getMinimumStock() == null) {
                continue;
            }

            boolean existingPO = purchaseOrderRepository.existsPendingOrderForProduct(
                    branch.getId(), product.getId());
            if (existingPO) {
                continue;
            }

            List<Supplier> suppliers = supplierRepository.findByIsActiveTrue();
            if (suppliers.isEmpty()) {
                log.warn("No active suppliers found for auto-reorder");
                continue;
            }
            Supplier supplier = suppliers.get(0);

            List<User> admins = userRepository.findByRole(Role.ADMIN);
            if (admins.isEmpty()) {
                log.warn("No admin users found for auto-reorder");
                continue;
            }
            User systemUser = admins.get(0);

            int reorderQty = product.getReorderLevel() * 2 - item.getQuantityOnHand();
            if (reorderQty <= 0) reorderQty = product.getReorderLevel();

            PurchaseOrder po = PurchaseOrder.builder()
                    .poNumber(generateAutoPoNumber())
                    .supplier(supplier)
                    .branch(branch)
                    .orderedBy(systemUser)
                    .orderDate(LocalDateTime.now())
                    .expectedDeliveryDate(LocalDate.now().plusDays(
                            supplier.getLeadTimeDays() != null ? supplier.getLeadTimeDays() : 7))
                    .status(PurchaseOrderStatus.DRAFT)
                    .taxAmount(BigDecimal.ZERO)
                    .shippingCost(BigDecimal.ZERO)
                    .notes("Auto-generated reorder for low stock product: " + product.getName())
                    .build();

            PurchaseOrderItem poItem = PurchaseOrderItem.builder()
                    .product(product)
                    .quantityOrdered(reorderQty)
                    .unitCost(product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO)
                    .lineTotal(product.getCostPrice() != null
                            ? product.getCostPrice().multiply(BigDecimal.valueOf(reorderQty))
                            : BigDecimal.ZERO)
                    .build();
            po.addItem(poItem);
            po.recalculateTotals();

            purchaseOrderRepository.save(po);
            poCreated++;

            notificationService.createNotificationForAllAdmins(
                    NotificationType.AUTO_REORDER,
                    NotificationPriority.MEDIUM,
                    "Auto-Reorder PO Created",
                    String.format("Draft PO %s created for '%s' at %s. Qty: %d. Please review and submit.",
                            po.getPoNumber(), product.getName(), branch.getName(), reorderQty)
            );
        }

        log.info("Auto-reorder check complete. {} draft POs created.", poCreated);
    }

    private String generateAutoPoNumber() {
        long count = purchaseOrderRepository.count() + 1;
        return String.format("AUTO-PO-%06d", count);
    }
}