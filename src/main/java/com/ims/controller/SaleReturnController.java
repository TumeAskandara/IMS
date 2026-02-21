package com.ims.controller;

import com.ims.dto.response.ApiResponse;
import com.ims.dto.returns.SaleReturnDTO;
import com.ims.dto.returns.SaleReturnRequest;
import com.ims.entity.User;
import com.ims.enums.ReturnStatus;
import com.ims.repository.UserRepository;
import com.ims.service.SaleReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Returns & Refunds", description = "APIs for managing product returns and refunds")
public class SaleReturnController {

    private final SaleReturnService returnService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Create return", description = "Create a new product return request")
    public ResponseEntity<ApiResponse<SaleReturnDTO>> createReturn(
            @Valid @RequestBody SaleReturnRequest request,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        SaleReturnDTO returnDTO = returnService.createReturn(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Return created successfully", returnDTO));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Get return by ID", description = "Retrieve return details by ID")
    public ResponseEntity<ApiResponse<SaleReturnDTO>> getReturnById(@PathVariable Long id) {
        SaleReturnDTO returnDTO = returnService.getReturnById(id);
        return ResponseEntity.ok(ApiResponse.success(returnDTO));
    }

    @GetMapping("/number/{returnNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Get return by number", description = "Retrieve return by return number")
    public ResponseEntity<ApiResponse<SaleReturnDTO>> getReturnByNumber(@PathVariable String returnNumber) {
        SaleReturnDTO returnDTO = returnService.getReturnByNumber(returnNumber);
        return ResponseEntity.ok(ApiResponse.success(returnDTO));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all returns", description = "Retrieve paginated list of all returns")
    public ResponseEntity<ApiResponse<Page<SaleReturnDTO>>> getAllReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<SaleReturnDTO> returns = returnService.getAllReturns(pageable);
        return ResponseEntity.ok(ApiResponse.success(returns));
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Get returns by branch", description = "Retrieve returns for a specific branch")
    public ResponseEntity<ApiResponse<Page<SaleReturnDTO>>> getReturnsByBranch(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<SaleReturnDTO> returns = returnService.getReturnsByBranch(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.success(returns));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get returns by status", description = "Retrieve returns filtered by status")
    public ResponseEntity<ApiResponse<Page<SaleReturnDTO>>> getReturnsByStatus(
            @PathVariable ReturnStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<SaleReturnDTO> returns = returnService.getReturnsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(returns));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Approve return", description = "Approve a pending return request")
    public ResponseEntity<ApiResponse<SaleReturnDTO>> approveReturn(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        SaleReturnDTO returnDTO = returnService.approveReturn(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Return approved successfully", returnDTO));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Reject return", description = "Reject a pending return request")
    public ResponseEntity<ApiResponse<SaleReturnDTO>> rejectReturn(
            @PathVariable Long id,
            @RequestParam String reason,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        SaleReturnDTO returnDTO = returnService.rejectReturn(id, userId, reason);
        return ResponseEntity.ok(ApiResponse.success("Return rejected", returnDTO));
    }

    @PostMapping("/{id}/process-refund")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Process refund", description = "Process refund and restock items")
    public ResponseEntity<ApiResponse<SaleReturnDTO>> processRefund(@PathVariable Long id) {
        SaleReturnDTO returnDTO = returnService.processRefund(id);
        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", returnDTO));
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        // Get username from Spring Security's UserDetails
        String username = authentication.getName();

        // Fetch the actual User entity from database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return user.getId();
    }
}