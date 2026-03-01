package com.ims.controller;

import com.ims.dto.purchaseorder.GoodsReceiptRequest;
import com.ims.dto.purchaseorder.PurchaseOrderDTO;
import com.ims.dto.purchaseorder.PurchaseOrderRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.enums.PurchaseOrderStatus;
import com.ims.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Purchase Orders", description = "APIs for managing purchase orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create purchase order")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequest request) {
        PurchaseOrderDTO po = purchaseOrderService.createPurchaseOrder(request);
        return ResponseEntity.ok(ApiResponse.success("Purchase order created", po));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get purchase order by ID")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> getPurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(purchaseOrderService.getPurchaseOrderById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all purchase orders")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderDTO>>> getAllPurchaseOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                purchaseOrderService.getAllPurchaseOrders(PageRequest.of(page, size))));
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get purchase orders by branch")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderDTO>>> getByBranch(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                purchaseOrderService.getPurchaseOrdersByBranch(branchId, PageRequest.of(page, size))));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get purchase orders by status")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderDTO>>> getByStatus(
            @PathVariable PurchaseOrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                purchaseOrderService.getPurchaseOrdersByStatus(status, PageRequest.of(page, size))));
    }

    @GetMapping("/supplier/{supplierId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get purchase orders by supplier")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderDTO>>> getBySupplier(
            @PathVariable Long supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                purchaseOrderService.getPurchaseOrdersBySupplier(supplierId, PageRequest.of(page, size))));
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Submit purchase order for approval")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> submitPurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Purchase order submitted",
                purchaseOrderService.submitPurchaseOrder(id)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Approve purchase order")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> approvePurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Purchase order approved",
                purchaseOrderService.approvePurchaseOrder(id)));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Receive goods against purchase order")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> receiveGoods(
            @PathVariable Long id, @Valid @RequestBody GoodsReceiptRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Goods received successfully",
                purchaseOrderService.receiveGoods(id, request)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cancel purchase order")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> cancelPurchaseOrder(
            @PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success("Purchase order cancelled",
                purchaseOrderService.cancelPurchaseOrder(id, reason)));
    }
}
