package com.ims.service;

import com.ims.dto.request.SaleItemRequest;
import com.ims.dto.request.SaleRequest;
import com.ims.entity.*;
import com.ims.enums.*;
import com.ims.exception.BadRequestException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    private final CustomerRepository customerRepository;

    @Transactional
    public Sale createSale(SaleRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", request.getBranchId()));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Validate customer if provided
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()));

            // Blacklist check: reject sales to BLACKLISTED customers (any payment method)
            if (customer.getStatus() == CustomerStatus.BLACKLISTED) {
                throw new BadRequestException(
                        "Cannot create sale for blacklisted customer: " + customer.getName());
            }

            // Suspended check: reject credit sales to SUSPENDED customers
            if (request.getPaymentMethod() == PaymentMethod.CREDIT
                    && customer.getStatus() == CustomerStatus.SUSPENDED) {
                throw new BadRequestException(
                        "Cannot create credit sale for suspended customer: " + customer.getName());
            }
        }

        // Validate sale-level amounts
        if (request.getTaxAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Tax amount cannot be negative");
        }
        if (request.getDiscountAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Sale discount amount cannot be negative");
        }
        if (request.getAmountPaid().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Amount paid cannot be negative");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Sale must have at least one item");
        }

        // Calculate totals
        BigDecimal subtotal = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();

        for (SaleItemRequest itemReq : request.getItems()) {
            // Validate item-level amounts
            if (itemReq.getQuantity() <= 0) {
                throw new BadRequestException("Quantity must be greater than zero");
            }
            if (itemReq.getUnitPrice() == null || itemReq.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Unit price must be greater than zero");
            }
            if (itemReq.getDiscountAmount() == null) {
                itemReq.setDiscountAmount(BigDecimal.ZERO);
            }
            if (itemReq.getDiscountAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Item discount amount cannot be negative");
            }

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

            // Validate discount doesn't exceed line total
            if (lineTotal.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException(
                        "Discount amount exceeds line total for product " + product.getName());
            }

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

        // Credit limit enforcement: before creating a credit sale, check available credit
        if (request.getPaymentMethod() == PaymentMethod.CREDIT
                && amountDue.compareTo(BigDecimal.ZERO) > 0
                && customer != null) {
            if (customer.getAvailableCredit().compareTo(amountDue) < 0) {
                throw new BadRequestException(
                        "Credit limit exceeded for customer " + customer.getName() +
                        ". Available credit: " + customer.getAvailableCredit() +
                        ", Amount due: " + amountDue);
            }
        }

        // Determine sale status
        SaleStatus status;
        if (request.getPaymentMethod() == PaymentMethod.CREDIT) {
            status = amountDue.compareTo(BigDecimal.ZERO) > 0 ? SaleStatus.PENDING : SaleStatus.COMPLETED;
        } else {
            status = SaleStatus.COMPLETED;
        }

        Sale sale = Sale.builder()
                .invoiceNumber(generateInvoiceNumber(branch))
                .branch(branch)
                .seller(seller)
                .customer(customer)
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
            createDebtRecord(savedSale, request.getCreditAccountId(), request.getDueDate(), customer);
        }

        return savedSale;
    }

    private void updateInventoryForSale(Branch branch, Product product, Integer quantity, Long saleId) {
        BranchInventory inventory = branchInventoryRepository
                .findByBranchIdAndProductIdForUpdate(branch.getId(), product.getId())
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

    private void createDebtRecord(Sale sale, Long creditAccountId, String dueDateStr, Customer customer) {
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

        // Update Customer.currentDebt so credit limit enforcement stays accurate
        if (customer != null) {
            customer.addDebt(sale.getAmountDue());
            customerRepository.save(customer);
        }
    }

    private String generateInvoiceNumber(Branch branch) {
        LocalDateTime yearStart = LocalDateTime.now().withMonth(1).withDayOfMonth(1).toLocalDate().atStartOfDay();
        Long count = saleRepository.getYearlySalesCount(branch.getId(), yearStart);
        long sequence = count + 1;
        return String.format("%s-INV-%d-%05d",
                branch.getCode(),
                LocalDateTime.now().getYear(),
                sequence);
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