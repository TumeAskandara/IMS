package com.ims.controller;

import com.ims.dto.request.DebtPaymentRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.entity.Debt;
import com.ims.entity.DebtPayment;
import com.ims.enums.DebtStatus;
import com.ims.service.DebtService;
import com.ims.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/debts")
@RequiredArgsConstructor
@Tag(name = "Debt Management", description = "Debt tracking and payment operations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SELLER')")
public class DebtController {

    private final DebtService debtService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all debts", description = "List all debts with filters")
    public ResponseEntity<ApiResponse<Page<Debt>>> getAllDebts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Long branchId = securityUtils.resolveBranchId(null);
        Page<Debt> debts;
        if (branchId != null) {
            debts = debtService.getDebtsByBranch(branchId, pageable);
        } else {
            debts = debtService.getAllDebts(pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(debts));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get debt details", description = "Get debt with payment history")
    public ResponseEntity<ApiResponse<Debt>> getDebtById(@PathVariable Long id) {
        Debt debt = debtService.getDebtById(id);
        securityUtils.validateBranchAccess(debt.getSale().getBranch().getId());
        return ResponseEntity.ok(ApiResponse.success(debt));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get overdue debts")
    public ResponseEntity<ApiResponse<List<Debt>>> getOverdueDebts() {
        Long branchId = securityUtils.resolveBranchId(null);
        List<Debt> debts;
        if (branchId != null) {
            debts = debtService.getOverdueDebtsByBranch(branchId);
        } else {
            debts = debtService.getOverdueDebts();
        }
        return ResponseEntity.ok(ApiResponse.success(debts));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get debts by status")
    public ResponseEntity<ApiResponse<Page<Debt>>> getDebtsByStatus(
            @PathVariable DebtStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Long branchId = securityUtils.resolveBranchId(null);
        Page<Debt> debts;
        if (branchId != null) {
            debts = debtService.getDebtsByStatusAndBranch(status, branchId, pageable);
        } else {
            debts = debtService.getDebtsByStatus(status, pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(debts));
    }

    @PostMapping("/{id}/payments")
    @Operation(summary = "Record payment", description = "Record payment against a debt")
    public ResponseEntity<ApiResponse<DebtPayment>> recordPayment(
            @PathVariable Long id,
            @Valid @RequestBody DebtPaymentRequest request
    ) {
        Debt debt = debtService.getDebtById(id);
        securityUtils.validateBranchAccess(debt.getSale().getBranch().getId());
        DebtPayment payment = debtService.recordPayment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Payment recorded successfully", payment));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get debt summary", description = "Total outstanding, overdue, etc.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDebtSummary() {
        Long branchId = securityUtils.resolveBranchId(null);
        Map<String, Object> summary;
        if (branchId != null) {
            summary = debtService.getDebtSummaryForBranch(branchId);
        } else {
            summary = debtService.getDebtSummary();
        }
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
