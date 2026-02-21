package com.ims.service;

import com.ims.dto.request.SaleItemRequest;
import com.ims.dto.request.SaleRequest;
import com.ims.entity.*;
import com.ims.enums.DebtStatus;
import com.ims.enums.PaymentMethod;
import com.ims.enums.SaleStatus;
import com.ims.enums.StockMovementType;
import com.ims.exception.BadRequestException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final DebtRepository debtRepository;

    @Transactional
    public Sale createSale(SaleRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", request.getBranchId()));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Calculate totals
        BigDecimal subtotal = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();

        for (SaleItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemReq.getProductId()));

            // Check stock availability
            BranchInventory inventory = branchInventoryRepository
                    .findByBranchIdAndProductId(branch.getId(), product.getId())
                    .orElseThrow(() -> new BadRequestException(
                            "Product " + product.getName() + " not available in this branch"));

            if (inventory.getQuantityAvailable() < itemReq.getQuantity()) {
                throw new BadRequestException(
                        "Insufficient stock for " + product.getName() +
                                ". Available: " + inventory.getQuantityAvailable());
            }

            BigDecimal lineTotal = itemReq.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()))
                    .subtract(itemReq.getDiscountAmount());

            SaleItem saleItem = SaleItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .discountAmount(itemReq.getDiscountAmount())
                    .lineTotal(lineTotal)
                    .build();

            saleItems.add(saleItem);
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal totalAmount = subtotal
                .add(request.getTaxAmount())
                .subtract(request.getDiscountAmount());

        BigDecimal amountDue = totalAmount.subtract(request.getAmountPaid());

        // Determine sale status
        SaleStatus status;
        if (request.getPaymentMethod() == PaymentMethod.CREDIT) {
            status = amountDue.compareTo(BigDecimal.ZERO) > 0 ? SaleStatus.PENDING : SaleStatus.COMPLETED;
        } else {
            status = SaleStatus.COMPLETED;
        }

        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber(branch);

        Sale sale = Sale.builder()
                .invoiceNumber(invoiceNumber)
                .branch(branch)
                .seller(seller)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .saleDate(LocalDateTime.now())
                .subtotal(subtotal)
                .taxAmount(request.getTaxAmount())
                .discountAmount(request.getDiscountAmount())
                .totalAmount(totalAmount)
                .amountPaid(request.getAmountPaid())
                .amountDue(amountDue)
                .paymentMethod(request.getPaymentMethod())
                .status(status)
                .notes(request.getNotes())
                .build();

        // Add sale items
        for (SaleItem item : saleItems) {
            sale.addSaleItem(item);
        }

        Sale savedSale = saleRepository.save(sale);

        // Update inventory and create stock movements
        for (SaleItem item : savedSale.getSaleItems()) {
            updateInventoryForSale(branch, item.getProduct(), item.getQuantity(), savedSale.getId());
        }

        // Handle credit sale
        if (request.getPaymentMethod() == PaymentMethod.CREDIT &&
                amountDue.compareTo(BigDecimal.ZERO) > 0) {
            createDebtRecord(savedSale, request.getCreditAccountId(), request.getDueDate());
        }

        return savedSale;
    }

    private void updateInventoryForSale(Branch branch, Product product, Integer quantity, Long saleId) {
        BranchInventory inventory = branchInventoryRepository
                .findByBranchIdAndProductId(branch.getId(), product.getId())
                .orElseThrow(() -> new BadRequestException("Inventory not found"));

        int oldQuantity = inventory.getQuantityOnHand();
        int newQuantity = oldQuantity - quantity;

        inventory.setQuantityOnHand(newQuantity);
        inventory.setQuantityAvailable(newQuantity - inventory.getQuantityReserved());
        branchInventoryRepository.save(inventory);

        // Create stock movement
        StockMovement movement = StockMovement.builder()
                .product(product)
                .branch(branch)
                .movementType(StockMovementType.SALE)
                .quantity(quantity)
                .quantityBefore(oldQuantity)
                .quantityAfter(newQuantity)
                .referenceType("SALE")
                .referenceId(saleId)
                .notes("Sale transaction")
                .build();
        stockMovementRepository.save(movement);
    }

    private void createDebtRecord(Sale sale, Long creditAccountId, String dueDateStr) {
        CreditAccount creditAccount;
        if (creditAccountId != null) {
            creditAccount = creditAccountRepository.findById(creditAccountId)
                    .orElseThrow(() -> new ResourceNotFoundException("CreditAccount", "id", creditAccountId));
        } else {
            // Create new credit account for this customer
            creditAccount = CreditAccount.builder()
                    .accountNumber("CA-" + System.currentTimeMillis())
                    .customerName(sale.getCustomerName())
                    .customerPhone(sale.getCustomerPhone())
                    .customerEmail(sale.getCustomerEmail())
                    .totalCreditUsed(BigDecimal.ZERO)
                    .isBlacklisted(false)
                    .build();
            creditAccount = creditAccountRepository.save(creditAccount);
        }

        LocalDate dueDate = dueDateStr != null ? LocalDate.parse(dueDateStr) : LocalDate.now().plusDays(30);

        Debt debt = Debt.builder()
                .creditAccount(creditAccount)
                .sale(sale)
                .totalAmount(sale.getTotalAmount())
                .amountPaid(sale.getAmountPaid())
                .balanceDue(sale.getAmountDue())
                .dueDate(dueDate)
                .status(DebtStatus.PENDING)
                .build();

        debtRepository.save(debt);

        // Update credit account total
        creditAccount.setTotalCreditUsed(
                creditAccount.getTotalCreditUsed().add(sale.getAmountDue()));
        creditAccountRepository.save(creditAccount);
    }

    private String generateInvoiceNumber(Branch branch) {
        Long count = saleRepository.getTodaySalesCount(branch.getId(), LocalDateTime.now().toLocalDate().atStartOfDay());
        return String.format("%s-INV-%d-%05d",
                branch.getCode(),
                LocalDateTime.now().getYear(),
                count + 1);
    }

    // ==========================================
    // QUERY METHODS (With Eager Loading)
    // ==========================================

    /**
     * Get all sales with all relationships loaded
     * Uses JOIN FETCH to prevent lazy loading errors
     */
    @Transactional(readOnly = true)
    public Page<Sale> getAllSales(Pageable pageable) {
        return saleRepository.findAllWithItems(pageable);
    }

    /**
     * Get sale by ID with all relationships loaded
     * Uses JOIN FETCH to prevent lazy loading errors
     */
    @Transactional(readOnly = true)
    public Sale getSaleById(Long id) {
        return saleRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "id", id));
    }

    /**
     * Get sales by branch with all relationships loaded
     * Uses JOIN FETCH to prevent lazy loading errors
     */
    @Transactional(readOnly = true)
    public Page<Sale> getSalesByBranch(Long branchId, Pageable pageable) {
        return saleRepository.findByBranchIdWithItems(branchId, pageable);
    }

    /**
     * Get sales by status with all relationships loaded
     * Uses JOIN FETCH to prevent lazy loading errors
     */
    @Transactional(readOnly = true)
    public Page<Sale> getSalesByStatus(SaleStatus status, Pageable pageable) {
        return saleRepository.findByStatusWithItems(status, pageable);
    }

    /**
     * Get sales by customer with all relationships loaded
     * Uses JOIN FETCH to prevent lazy loading errors
     */
    @Transactional(readOnly = true)
    public Page<Sale> getSalesByCustomer(Customer customer, Pageable pageable) {
        return saleRepository.findByCustomerWithItems(customer, pageable);
    }
}