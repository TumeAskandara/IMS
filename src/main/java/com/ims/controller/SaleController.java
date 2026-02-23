package com.ims.controller;

import com.ims.dto.request.SaleRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.entity.Sale;
import com.ims.service.SaleService;
import com.ims.util.SecurityUtils;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
@Tag(name = "Sales Management", description = "Sales and transaction operations")
@SecurityRequirement(name = "bearerAuth")
public class SaleController {

    private final SaleService saleService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Create sale", description = "Create new sale transaction")
    public ResponseEntity<ApiResponse<Sale>> createSale(@Valid @RequestBody SaleRequest request) {
        securityUtils.validateBranchAccess(request.getBranchId());
        Sale sale = saleService.createSale(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sale created successfully", sale));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all sales", description = "List sales with filters")
    public ResponseEntity<ApiResponse<Page<Sale>>> getAllSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "saleDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Long branchId = securityUtils.resolveBranchId(null);
        Page<Sale> sales;
        if (branchId != null) {
            sales = saleService.getSalesByBranch(branchId, pageable);
        } else {
            sales = saleService.getAllSales(pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(sales));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SELLER')")
    @Operation(summary = "Get sale details", description = "Get sale by ID with all items")
    public ResponseEntity<ApiResponse<Sale>> getSaleById(@PathVariable Long id) {
        Sale sale = saleService.getSaleById(id);
        securityUtils.validateBranchAccess(sale.getBranch().getId());
        return ResponseEntity.ok(ApiResponse.success(sale));
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get branch sales", description = "Get sales for specific branch")
    public ResponseEntity<ApiResponse<Page<Sale>>> getSalesByBranch(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        securityUtils.validateBranchAccess(branchId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Sale> sales = saleService.getSalesByBranch(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.success(sales));
    }
}
