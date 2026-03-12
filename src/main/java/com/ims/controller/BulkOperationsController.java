package com.ims.controller;

import com.ims.dto.response.ApiResponse;
import com.ims.service.BulkOperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bulk-operations")
@RequiredArgsConstructor
@Tag(name = "Batch/Bulk Operations", description = "Perform bulk operations on products")
@SecurityRequirement(name = "bearerAuth")
public class BulkOperationsController {

    private final BulkOperationsService bulkOperationsService;

    @PutMapping("/prices")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Bulk update prices", description = "Update prices for multiple products at once")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkUpdatePrices(
            @RequestBody List<Map<String, Object>> updates) {
        Map<String, Object> result = bulkOperationsService.bulkUpdatePrices(updates);
        return ResponseEntity.ok(ApiResponse.success("Bulk price update completed", result));
    }

    @PutMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Bulk update product status", description = "Activate or deactivate multiple products")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkUpdateStatus(
            @RequestParam List<Long> productIds,
            @RequestParam boolean isActive) {
        Map<String, Object> result = bulkOperationsService.bulkUpdateStatus(productIds, isActive);
        return ResponseEntity.ok(ApiResponse.success("Bulk status update completed", result));
    }

    @DeleteMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk delete products", description = "Soft-delete multiple products at once")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkDelete(
            @RequestParam List<Long> productIds) {
        Map<String, Object> result = bulkOperationsService.bulkDelete(productIds);
        return ResponseEntity.ok(ApiResponse.success("Bulk delete completed", result));
    }

    @PutMapping("/price-adjustment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Bulk price adjustment", description = "Apply percentage price change to multiple products")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkPriceAdjustment(
            @RequestParam List<Long> productIds,
            @RequestParam BigDecimal percentageChange) {
        Map<String, Object> result = bulkOperationsService.bulkApplyPriceAdjustment(productIds, percentageChange);
        return ResponseEntity.ok(ApiResponse.success("Price adjustment applied", result));
    }
}