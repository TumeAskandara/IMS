package com.ims.controller;

import com.ims.dto.response.ApiResponse;
import com.ims.entity.Customer;
import com.ims.entity.Product;
import com.ims.repository.BranchInventoryRepository;
import com.ims.repository.CategoryRepository;
import com.ims.repository.CustomerRepository;
import com.ims.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
@Tag(name = "Offline Mode / Data Sync", description = "Endpoints for offline data synchronization")
@SecurityRequirement(name = "bearerAuth")
public class SyncController {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping("/snapshot")
    @Operation(summary = "Get data snapshot for offline use",
            description = "Returns essential data (products, customers, categories, inventory) for offline caching")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDataSnapshot(
            @RequestParam Long branchId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("generatedAt", LocalDateTime.now());
        snapshot.put("branchId", branchId);
        snapshot.put("products", productRepository.findByIsDeletedFalse(PageRequest.of(0, 10000)).getContent());
        snapshot.put("categories", categoryRepository.findByIsDeletedFalse());
        snapshot.put("customers", customerRepository.findByBranchId(branchId, PageRequest.of(0, 10000)).getContent());
        snapshot.put("inventory", branchInventoryRepository.findByBranchId(branchId));

        return ResponseEntity.ok(ApiResponse.success(snapshot));
    }

    @GetMapping("/changes")
    @Operation(summary = "Get changes since timestamp",
            description = "Returns entities modified after the given timestamp for incremental sync")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChangesSince(
            @RequestParam String since,
            @RequestParam Long branchId) {
        LocalDateTime sinceDate = LocalDateTime.parse(since);
        Map<String, Object> changes = new LinkedHashMap<>();

        changes.put("since", sinceDate);
        changes.put("generatedAt", LocalDateTime.now());

        List<Product> allProducts = productRepository.findAll();
        List<Product> updatedProducts = allProducts.stream()
                .filter(p -> p.getUpdatedAt() != null && p.getUpdatedAt().isAfter(sinceDate))
                .toList();
        changes.put("products", updatedProducts);

        List<Customer> branchCustomers = customerRepository.findByBranchId(branchId, PageRequest.of(0, 10000)).getContent();
        List<Customer> updatedCustomers = branchCustomers.stream()
                .filter(c -> c.getUpdatedAt() != null && c.getUpdatedAt().isAfter(sinceDate))
                .toList();
        changes.put("customers", updatedCustomers);

        changes.put("inventory", branchInventoryRepository.findByBranchId(branchId));

        return ResponseEntity.ok(ApiResponse.success(changes));
    }
}