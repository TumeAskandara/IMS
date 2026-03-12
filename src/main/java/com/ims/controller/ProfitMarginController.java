package com.ims.controller;

import com.ims.dto.response.ApiResponse;
import com.ims.service.ProfitMarginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/profit-margins")
@RequiredArgsConstructor
@Tag(name = "Profit Margin Warnings", description = "Monitor and warn about low profit margins")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ProfitMarginController {

    private final ProfitMarginService profitMarginService;

    @GetMapping("/low")
    @Operation(summary = "Get products with low margins",
            description = "Returns all products with profit margin below threshold")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLowMarginProducts() {
        List<Map<String, Object>> products = profitMarginService.getProductsWithLowMargins();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Check margin for a specific product")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkProductMargin(
            @PathVariable Long productId) {
        Map<String, Object> result = profitMarginService.checkProductMargin(productId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}