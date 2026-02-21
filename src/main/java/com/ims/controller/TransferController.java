package com.ims.controller;

import com.ims.dto.request.TransferRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.dto.response.StockTransferDTO;
import com.ims.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfer Management", description = "Inter-branch stock transfer operations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @Operation(summary = "Create transfer", description = "Create new transfer request")
    public ResponseEntity<ApiResponse<StockTransferDTO>> createTransfer(
            @Valid @RequestBody TransferRequest request
    ) {
        StockTransferDTO transfer = transferService.createTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer created successfully", transfer));
    }

    @GetMapping
    @Operation(summary = "Get all transfers", description = "List all transfers with filters")
    public ResponseEntity<ApiResponse<Page<StockTransferDTO>>> getAllTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StockTransferDTO> transfers = transferService.getAllTransfers(pageable);
        return ResponseEntity.ok(ApiResponse.success(transfers));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transfer details")
    public ResponseEntity<ApiResponse<StockTransferDTO>> getTransferById(@PathVariable Long id) {
        StockTransferDTO transfer = transferService.getTransferById(id);
        return ResponseEntity.ok(ApiResponse.success(transfer));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve transfer")
    public ResponseEntity<ApiResponse<StockTransferDTO>> approveTransfer(@PathVariable Long id) {
        StockTransferDTO transfer = transferService.approveTransfer(id);
        return ResponseEntity.ok(ApiResponse.success("Transfer approved", transfer));
    }

    @PatchMapping("/{id}/ship")
    @Operation(summary = "Mark as shipped")
    public ResponseEntity<ApiResponse<StockTransferDTO>> shipTransfer(@PathVariable Long id) {
        StockTransferDTO transfer = transferService.shipTransfer(id);
        return ResponseEntity.ok(ApiResponse.success("Transfer shipped", transfer));
    }

    @PatchMapping("/{id}/receive")
    @Operation(summary = "Confirm receipt", description = "Confirm receipt with quantities")
    public ResponseEntity<ApiResponse<StockTransferDTO>> receiveTransfer(
            @PathVariable Long id,
            @RequestBody List<Integer> receivedQuantities
    ) {
        StockTransferDTO transfer = transferService.receiveTransfer(id, receivedQuantities);
        return ResponseEntity.ok(ApiResponse.success("Transfer received", transfer));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject transfer")
    public ResponseEntity<ApiResponse<StockTransferDTO>> rejectTransfer(
            @PathVariable Long id,
            @RequestParam String reason
    ) {
        StockTransferDTO transfer = transferService.rejectTransfer(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Transfer rejected", transfer));
    }
}