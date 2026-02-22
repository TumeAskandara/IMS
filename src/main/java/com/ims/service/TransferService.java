package com.ims.service;

import com.ims.dto.request.TransferItemRequest;
import com.ims.dto.request.TransferRequest;
import com.ims.dto.response.StockTransferDTO;
import com.ims.dto.response.TransferItemDTO;
import com.ims.entity.*;
import com.ims.enums.NotificationPriority;
import com.ims.enums.NotificationType;
import com.ims.enums.Role;
import com.ims.enums.StockMovementType;
import com.ims.enums.TransferStatus;
import com.ims.exception.BadRequestException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final StockTransferRepository stockTransferRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final NotificationService notificationService;

    @Transactional
    public StockTransferDTO createTransfer(TransferRequest request) {
        Branch sourceBranch = branchRepository.findById(request.getSourceBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", request.getSourceBranchId()));

        Branch destinationBranch = branchRepository.findById(request.getDestinationBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", request.getDestinationBranchId()));

        if (sourceBranch.getId().equals(destinationBranch.getId())) {
            throw new BadRequestException("Source and destination branches cannot be the same");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User requestedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        String transferNumber = generateTransferNumber(sourceBranch, destinationBranch);

        StockTransfer transfer = StockTransfer.builder()
                .transferNumber(transferNumber)
                .sourceBranch(sourceBranch)
                .destinationBranch(destinationBranch)
                .requestedBy(requestedBy)
                .requestDate(LocalDateTime.now())
                .status(TransferStatus.PENDING)
                .notes(request.getNotes())
                .build();

        List<TransferItem> items = new ArrayList<>();
        for (TransferItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemReq.getProductId()));

            TransferItem item = TransferItem.builder()
                    .product(product)
                    .quantityRequested(itemReq.getQuantityRequested())
                    .build();

            items.add(item);
        }

        for (TransferItem item : items) {
            transfer.addTransferItem(item);
        }

        StockTransfer saved = stockTransferRepository.save(transfer);

        // Map to DTO while session is still open
        return mapToDTO(saved);
    }

    @Transactional
    public StockTransferDTO approveTransfer(Long transferId) {
        StockTransfer transfer = getTransferEntityById(transferId);

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new BadRequestException("Transfer is not in pending status");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User approvedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Validate stock availability for ALL items first (before reserving any)
        List<BranchInventory> inventoriesToReserve = new ArrayList<>();
        for (TransferItem item : transfer.getTransferItems()) {
            BranchInventory inventory = branchInventoryRepository
                    .findByBranchIdAndProductIdForUpdate(transfer.getSourceBranch().getId(), item.getProduct().getId())
                    .orElseThrow(() -> new BadRequestException(
                            "Product " + item.getProduct().getName() + " not available in source branch"));

            if (inventory.getQuantityAvailable() < item.getQuantityRequested()) {
                throw new BadRequestException(
                        "Insufficient stock for " + item.getProduct().getName() +
                                ". Available: " + inventory.getQuantityAvailable());
            }

            inventoriesToReserve.add(inventory);
        }

        // All validations passed — now reserve stock
        List<TransferItem> items = transfer.getTransferItems();
        for (int i = 0; i < items.size(); i++) {
            BranchInventory inventory = inventoriesToReserve.get(i);
            TransferItem item = items.get(i);

            inventory.setQuantityReserved(inventory.getQuantityReserved() + item.getQuantityRequested());
            inventory.setQuantityAvailable(inventory.getQuantityOnHand() - inventory.getQuantityReserved());
            branchInventoryRepository.save(inventory);
        }

        transfer.setStatus(TransferStatus.APPROVED);
        transfer.setApprovedBy(approvedBy);
        transfer.setApprovalDate(LocalDateTime.now());

        StockTransfer saved = stockTransferRepository.save(transfer);

        // Notify the user who created the transfer
        notificationService.createNotification(
                transfer.getRequestedBy().getId(),
                NotificationType.TRANSFER_APPROVED,
                NotificationPriority.MEDIUM,
                "Transfer Approved",
                String.format("Your transfer request #%s has been approved",
                        transfer.getTransferNumber())
        );

        // Notify destination branch manager
        List<User> destManagers = userRepository.findByBranchAndRole(
                transfer.getDestinationBranch(),
                Role.MANAGER
        );

        for (User manager : destManagers) {
            notificationService.createNotification(
                    manager.getId(),
                    NotificationType.TRANSFER_APPROVED,
                    NotificationPriority.HIGH,
                    "Incoming Transfer",
                    String.format("Transfer #%s from %s is on the way",
                            transfer.getTransferNumber(),
                            transfer.getSourceBranch().getName())
            );
        }

        return mapToDTO(saved);
    }

    @Transactional
    public StockTransferDTO shipTransfer(Long transferId) {
        StockTransfer transfer = getTransferEntityById(transferId);

        if (transfer.getStatus() != TransferStatus.APPROVED) {
            throw new BadRequestException("Transfer must be approved before shipping");
        }

        // Deduct stock from source
        for (TransferItem item : transfer.getTransferItems()) {
            BranchInventory inventory = branchInventoryRepository
                    .findByBranchIdAndProductIdForUpdate(transfer.getSourceBranch().getId(), item.getProduct().getId())
                    .orElseThrow();

            int oldQuantity = inventory.getQuantityOnHand();
            int newQuantity = oldQuantity - item.getQuantityRequested();

            inventory.setQuantityOnHand(newQuantity);
            inventory.setQuantityReserved(inventory.getQuantityReserved() - item.getQuantityRequested());
            inventory.setQuantityAvailable(newQuantity - inventory.getQuantityReserved());
            branchInventoryRepository.save(inventory);

            item.setQuantityShipped(item.getQuantityRequested());

            // Create stock movement
            StockMovement movement = StockMovement.builder()
                    .product(item.getProduct())
                    .branch(transfer.getSourceBranch())
                    .movementType(StockMovementType.TRANSFER_OUT)
                    .quantity(item.getQuantityRequested())
                    .quantityBefore(oldQuantity)
                    .quantityAfter(newQuantity)
                    .referenceType("TRANSFER")
                    .referenceId(transfer.getId())
                    .notes("Stock transfer to " + transfer.getDestinationBranch().getName())
                    .build();
            stockMovementRepository.save(movement);
        }

        transfer.setStatus(TransferStatus.SHIPPED);
        transfer.setShipDate(LocalDateTime.now());

        StockTransfer saved = stockTransferRepository.save(transfer);
        return mapToDTO(saved);
    }

    @Transactional
    public StockTransferDTO receiveTransfer(Long transferId, List<Integer> receivedQuantities) {
        StockTransfer transfer = getTransferEntityById(transferId);

        if (transfer.getStatus() != TransferStatus.SHIPPED) {
            throw new BadRequestException("Transfer must be shipped before receiving");
        }

        List<TransferItem> items = transfer.getTransferItems();
        if (receivedQuantities.size() != items.size()) {
            throw new BadRequestException("Received quantities count mismatch");
        }

        for (int i = 0; i < items.size(); i++) {
            TransferItem item = items.get(i);
            Integer receivedQty = receivedQuantities.get(i);

            item.setQuantityReceived(receivedQty);
            item.setQuantityDamaged(item.getQuantityShipped() - receivedQty);

            // Add stock to destination
            BranchInventory inventory = branchInventoryRepository
                    .findByBranchIdAndProductIdForUpdate(transfer.getDestinationBranch().getId(), item.getProduct().getId())
                    .orElse(BranchInventory.builder()
                            .branch(transfer.getDestinationBranch())
                            .product(item.getProduct())
                            .quantityOnHand(0)
                            .quantityReserved(0)
                            .quantityAvailable(0)
                            .build());

            int oldQuantity = inventory.getQuantityOnHand();
            int newQuantity = oldQuantity + receivedQty;

            inventory.setQuantityOnHand(newQuantity);
            inventory.setQuantityAvailable(newQuantity - inventory.getQuantityReserved());
            branchInventoryRepository.save(inventory);

            // Create stock movement
            StockMovement movement = StockMovement.builder()
                    .product(item.getProduct())
                    .branch(transfer.getDestinationBranch())
                    .movementType(StockMovementType.TRANSFER_IN)
                    .quantity(receivedQty)
                    .quantityBefore(oldQuantity)
                    .quantityAfter(newQuantity)
                    .referenceType("TRANSFER")
                    .referenceId(transfer.getId())
                    .notes("Stock transfer from " + transfer.getSourceBranch().getName())
                    .build();
            stockMovementRepository.save(movement);
        }

        transfer.setStatus(TransferStatus.RECEIVED);
        transfer.setReceiveDate(LocalDateTime.now());

        StockTransfer saved = stockTransferRepository.save(transfer);
        return mapToDTO(saved);
    }

    @Transactional
    public StockTransferDTO rejectTransfer(Long transferId, String reason) {
        StockTransfer transfer = getTransferEntityById(transferId);

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new BadRequestException("Only pending transfers can be rejected");
        }

        transfer.setStatus(TransferStatus.REJECTED);
        transfer.setRejectionReason(reason);

        StockTransfer saved = stockTransferRepository.save(transfer);

        // Notify the user who created the transfer
        notificationService.createNotification(
                transfer.getRequestedBy().getId(),
                NotificationType.TRANSFER_REJECTED,
                NotificationPriority.MEDIUM,
                "Transfer Rejected",
                String.format("Your transfer request #%s was rejected. Reason: %s",
                        transfer.getTransferNumber(),
                        reason)
        );

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public StockTransferDTO getTransferById(Long id) {
        StockTransfer transfer = getTransferEntityById(id);
        return mapToDTO(transfer);
    }

    @Transactional(readOnly = true)
    public Page<StockTransferDTO> getAllTransfers(Pageable pageable) {
        Page<StockTransfer> transfers = stockTransferRepository.findAll(pageable);
        return transfers.map(this::mapToDTO);
    }

    // Private helper methods

    private StockTransfer getTransferEntityById(Long id) {
        return stockTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransfer", "id", id));
    }

    private String generateTransferNumber(Branch source, Branch destination) {
        return String.format("TRF-%s-%s-%d",
                source.getCode(),
                destination.getCode(),
                System.currentTimeMillis());
    }

    // DTO Mapping methods

    private StockTransferDTO mapToDTO(StockTransfer transfer) {
        return StockTransferDTO.builder()
                .id(transfer.getId())
                .transferNumber(transfer.getTransferNumber())
                .sourceBranchId(transfer.getSourceBranch().getId())
                .sourceBranchName(transfer.getSourceBranch().getName())
                .destinationBranchId(transfer.getDestinationBranch().getId())
                .destinationBranchName(transfer.getDestinationBranch().getName())
                .status(transfer.getStatus())
                .requestedByName(transfer.getRequestedBy().getFullName())
                .requestDate(transfer.getRequestDate())
                .approvedByName(transfer.getApprovedBy() != null ? transfer.getApprovedBy().getFullName() : null)
                .approvalDate(transfer.getApprovalDate())
                .shipDate(transfer.getShipDate())
                .receiveDate(transfer.getReceiveDate())
                .rejectionReason(transfer.getRejectionReason())
                .notes(transfer.getNotes())
                // This accesses the lazy collection while the session is still open
                .transferItems(transfer.getTransferItems().stream()
                        .map(this::mapItemToDTO)
                        .collect(Collectors.toList()))
                .createdAt(transfer.getCreatedAt())
                .updatedAt(transfer.getUpdatedAt())
                .build();
    }

    private TransferItemDTO mapItemToDTO(TransferItem item) {
        return TransferItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productSku(item.getProduct().getSku())
                .quantityRequested(item.getQuantityRequested())
                .quantityShipped(item.getQuantityShipped())
                .quantityReceived(item.getQuantityReceived())
                .quantityDamaged(item.getQuantityDamaged())
                .notes(item.getNotes())
                .build();
    }
}