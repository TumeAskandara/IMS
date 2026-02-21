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
import java.time.LocalDateTime;
import java.util.List;
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
    private final UserRepository userRepository;
    private final MessageService messageService;

    public SaleReturnDTO createReturn(SaleReturnRequest request, Long userId) {
        log.info("Creating return for sale ID: {}", request.getSaleId());

        // Validate sale exists
        Sale sale = saleRepository.findById(request.getSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found"));

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Generate return number
        String returnNumber = generateReturnNumber();

        // Calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;

        // Create return - WITHOUT items first
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

        // Process return items
        for (SaleReturnRequest.ReturnItemRequest itemRequest : request.getItems()) {
            SaleItem saleItem = saleItemRepository.findById(itemRequest.getSaleItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sale item not found"));

            // Validate quantity
            if (itemRequest.getQuantity() > saleItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Return quantity cannot exceed original quantity");
            }

            BigDecimal itemSubtotal = saleItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            SaleReturnItem returnItem = new SaleReturnItem();
            returnItem.setSaleReturn(saleReturn);  // Set parent first
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
        log.info("Approving return: {}", returnId);

        SaleReturn saleReturn = returnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found"));

        if (!saleReturn.isPending()) {
            throw new IllegalStateException("Only pending returns can be approved");
        }

        User approver = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        saleReturn.approve(approver);
        SaleReturn saved = returnRepository.save(saleReturn);

        log.info("Return approved: {}", saved.getReturnNumber());
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

        log.info("Return rejected: {}", saved.getReturnNumber());
        return mapToDTO(saved);
    }

    public SaleReturnDTO processRefund(Long returnId) {
        log.info("Processing refund for return: {}", returnId);

        SaleReturn saleReturn = returnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found"));

        if (!saleReturn.canBeProcessed()) {
            throw new IllegalStateException("Return cannot be processed in current status");
        }

        // Restock items if applicable
        for (SaleReturnItem item : saleReturn.getItems()) {
            if (item.canBeRestocked() && !item.getRestocked()) {
                restockItem(item, saleReturn.getBranch());
                item.markAsRestocked();
            }
        }

        // Mark as completed
        saleReturn.complete();
        SaleReturn saved = returnRepository.save(saleReturn);

        log.info("Refund processed successfully: {}", saved.getReturnNumber());
        return mapToDTO(saved);
    }

    private void restockItem(SaleReturnItem returnItem, Branch branch) {
        BranchInventory inventory = inventoryRepository
                .findByBranchAndProduct(branch, returnItem.getProduct())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product in branch"));

        // Use the correct setter method
        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + returnItem.getQuantityReturned());
        inventory.setQuantityAvailable(inventory.getQuantityOnHand() - inventory.getQuantityReserved());
        inventoryRepository.save(inventory);

        log.info("Restocked {} units of product {} to branch {}",
                returnItem.getQuantityReturned(),
                returnItem.getProduct().getName(),
                branch.getName());
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
                .returnReason(item.getReturnReason())
                .condition(item.getCondition().name())
                .build();
    }
}