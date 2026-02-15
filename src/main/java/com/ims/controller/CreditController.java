package com.ims.controller;

import com.ims.dto.request.CreditAccountRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.entity.CreditAccount;
import com.ims.service.CreditService;
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

@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
@Tag(name = "Credit Management", description = "Customer credit account operations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class CreditController {

    private final CreditService creditService;

    @GetMapping
    @Operation(summary = "Get all credit accounts")
    public ResponseEntity<ApiResponse<Page<CreditAccount>>> getAllCreditAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CreditAccount> accounts = creditService.getAllCreditAccounts(pageable);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get credit account", description = "Get account with debt history")
    public ResponseEntity<ApiResponse<CreditAccount>> getCreditAccountById(@PathVariable Long id) {
        CreditAccount account = creditService.getCreditAccountById(id);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping
    @Operation(summary = "Create credit account", description = "Create new customer credit account")
    public ResponseEntity<ApiResponse<CreditAccount>> createCreditAccount(
            @Valid @RequestBody CreditAccountRequest request
    ) {
        CreditAccount account = creditService.createCreditAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Credit account created successfully", account));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update credit account")
    public ResponseEntity<ApiResponse<CreditAccount>> updateCreditAccount(
            @PathVariable Long id,
            @Valid @RequestBody CreditAccountRequest request
    ) {
        CreditAccount account = creditService.updateCreditAccount(id, request);
        return ResponseEntity.ok(ApiResponse.success("Credit account updated successfully", account));
    }

    @PatchMapping("/{id}/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Blacklist account", description = "Blacklist customer account")
    public ResponseEntity<ApiResponse<CreditAccount>> blacklistAccount(
            @PathVariable Long id,
            @RequestParam String reason
    ) {
        CreditAccount account = creditService.blacklistAccount(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Account blacklisted", account));
    }

    @PatchMapping("/{id}/unblacklist")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove blacklist")
    public ResponseEntity<ApiResponse<CreditAccount>> unblacklistAccount(@PathVariable Long id) {
        CreditAccount account = creditService.unblacklistAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Blacklist removed", account));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete credit account", description = "Soft delete credit account (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteCreditAccount(@PathVariable Long id) {
        creditService.deleteCreditAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Credit account deleted successfully", null));
    }
}
