package com.ims.controller;

import com.ims.dto.response.ApiResponse;
import com.ims.service.GlobalSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Global Search", description = "Search across all entities")
@SecurityRequirement(name = "bearerAuth")
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping
    @Operation(summary = "Global search", description = "Search products, customers, suppliers, and credit accounts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> results = globalSearchService.search(q, limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}