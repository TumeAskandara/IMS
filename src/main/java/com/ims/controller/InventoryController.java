package com.ims.controller;

import com.ims.dto.request.StockAdjustmentRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.entity.BranchInventory;
import com.ims.entity.StockMovement;
import com.ims.service.InventoryService;
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

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "Stock and inventory operations")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Get branch inventory", description = "Get all stock for a specific branch")
    public ResponseEntity<ApiResponse<Page<BranchInventory>>> getBranchInventory(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BranchInventory> inventory = inventoryService.getBranchInventory(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Adjust stock", description = "Manual stock adjustment")
    public ResponseEntity<ApiResponse<BranchInventory>> adjustStock(
            @RequestParam Long branchId,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        BranchInventory inventory = inventoryService.adjustStock(
                branchId,
                request.getProductId(),
                request.getQuantity(),
                request.getMovementType(),
                request.getNotes()
        );
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully", inventory));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get low stock items", description = "Get products below reorder threshold")
    public ResponseEntity<ApiResponse<List<BranchInventory>>> getLowStockItems(
            @RequestParam Long branchId
    ) {
        List<BranchInventory> items = inventoryService.getLowStockItems(branchId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/movements")
    @Operation(summary = "Get stock movements", description = "Get stock movement history")
    public ResponseEntity<ApiResponse<Page<StockMovement>>> getStockMovements(
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StockMovement> movements = inventoryService.getStockMovements(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.success(movements));
    }

    @GetMapping("/search")
    @Operation(summary = "Search inventory", description = "Search products by name or SKU in branch inventory")
    public ResponseEntity<ApiResponse<Page<BranchInventory>>> searchInventory(
            @RequestParam Long branchId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BranchInventory> results = inventoryService.searchInventory(branchId, query, pageable);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get inventory by category", description = "Get all products in a specific category for a branch")
    public ResponseEntity<ApiResponse<Page<BranchInventory>>> getInventoryByCategory(
            @RequestParam Long branchId,
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BranchInventory> inventory = inventoryService.getInventoryByCategory(branchId, categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(inventory));
    }

    @GetMapping("/in-stock")
    @Operation(summary = "Get in-stock items", description = "Get all products with available quantity > 0")
    public ResponseEntity<ApiResponse<Page<BranchInventory>>> getInStockItems(
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BranchInventory> items = inventoryService.getInStockItems(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/out-of-stock")
    @Operation(summary = "Get out-of-stock items", description = "Get all products with zero available quantity")
    public ResponseEntity<ApiResponse<Page<BranchInventory>>> getOutOfStockItems(
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BranchInventory> items = inventoryService.getOutOfStockItems(branchId, pageable);
        return ResponseEntity.ok(ApiResponse.success(items));
    }
}
