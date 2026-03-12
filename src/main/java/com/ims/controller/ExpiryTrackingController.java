package com.ims.controller;

import com.ims.dto.response.ApiResponse;
import com.ims.entity.Product;
import com.ims.service.ExpiryTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/expiry-tracking")
@RequiredArgsConstructor
@Tag(name = "Expiry Date Tracking", description = "Track product expiry dates")
@SecurityRequirement(name = "bearerAuth")
public class ExpiryTrackingController {

    private final ExpiryTrackingService expiryTrackingService;

    @GetMapping("/expiring-soon")
    @Operation(summary = "Get products expiring soon", description = "Products expiring within specified days")
    public ResponseEntity<ApiResponse<List<Product>>> getExpiringSoon(
            @RequestParam(defaultValue = "30") int days) {
        List<Product> products = expiryTrackingService.getProductsExpiringWithinDays(days);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/expired")
    @Operation(summary = "Get expired products")
    public ResponseEntity<ApiResponse<Page<Product>>> getExpiredProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Product> products = expiryTrackingService.getExpiredProductsPaged(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(products));
    }
}