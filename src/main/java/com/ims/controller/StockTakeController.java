package com.ims.controller;

import com.ims.dto.response.ApiResponse;
import com.ims.entity.StockTake;
import com.ims.service.StockTakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stock-takes")
@RequiredArgsConstructor
@Tag(name = "Stock Reconciliation", description = "Stock take / physical inventory count operations")
@SecurityRequirement(name = "bearerAuth")
public class StockTakeController {

    private final StockTakeService stockTakeService;

    @PostMapping("/initiate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Initiate stock take", description = "Start a new physical inventory count for a branch")
    public ResponseEntity<ApiResponse<StockTake>> initiateStockTake(
            @RequestParam Long branchId,
            @RequestParam(required = false) String notes) {
        StockTake stockTake = stockTakeService.initiateStockTake(branchId, notes);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Stock take initiated", stockTake));
    }

    @PutMapping("/{id}/count")
    @Operation(summary = "Record physical count", description = "Record the physical count for a stock take item")
    public ResponseEntity<ApiResponse<StockTake>> recordPhysicalCount(
            @PathVariable Long id,
            @RequestParam Long itemId,
            @RequestParam Integer physicalQuantity,
            @RequestParam(required = false) String notes) {
        StockTake stockTake = stockTakeService.updatePhysicalCount(id, itemId, physicalQuantity, notes);
        return ResponseEntity.ok(ApiResponse.success("Physical count recorded", stockTake));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Complete stock take", description = "Complete the stock take and optionally apply adjustments")
    public ResponseEntity<ApiResponse<StockTake>> completeStockTake(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean applyAdjustments) {
        StockTake stockTake = stockTakeService.completeStockTake(id, applyAdjustments);
        return ResponseEntity.ok(ApiResponse.success("Stock take completed", stockTake));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cancel stock take")
    public ResponseEntity<ApiResponse<StockTake>> cancelStockTake(@PathVariable Long id) {
        StockTake stockTake = stockTakeService.cancelStockTake(id);
        return ResponseEntity.ok(ApiResponse.success("Stock take cancelled", stockTake));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stock take by ID")
    public ResponseEntity<ApiResponse<StockTake>> getStockTake(@PathVariable Long id) {
        StockTake stockTake = stockTakeService.getStockTakeById(id);
        return ResponseEntity.ok(ApiResponse.success(stockTake));
    }

    @GetMapping
    @Operation(summary = "List all stock takes")
    public ResponseEntity<ApiResponse<Page<StockTake>>> getAllStockTakes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<StockTake> stockTakes = stockTakeService.getAllStockTakes(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(stockTakes));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "List stock takes by branch")
    public ResponseEntity<ApiResponse<Page<StockTake>>> getStockTakesByBranch(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<StockTake> stockTakes = stockTakeService.getStockTakesByBranch(branchId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(stockTakes));
    }
}