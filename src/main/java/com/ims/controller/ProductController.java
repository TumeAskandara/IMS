package com.ims.controller;

import com.ims.dto.request.ProductRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.entity.Product;
import com.ims.service.ProductService;
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

/**
 * Product Management Controller
 * 
 * Manages product catalog including spare parts inventory
 * Base URL: /api/v1/products
 * 
 * Access Control:
 * - GET endpoints: All authenticated users
 * - POST, PUT, DELETE: ADMIN and MANAGER roles only
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "Product/Spare Parts CRUD operations")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    /**
     * Get All Products (Paginated)
     * 
     * GET /api/v1/products?page=0&size=20&sort=name,asc
     * 
     * Query Parameters:
     * - page: Page number (default: 0)
     * - size: Items per page (default: 20)
     * - sort: Sort field,direction (default: id,asc)
     * 
     * Response:
     * {
     *   "success": true,
     *   "data": {
     *     "content": [...products...],
     *     "totalElements": 150,
     *     "totalPages": 8
     *   }
     * }
     */
    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieve paginated list of products")
    public ResponseEntity<ApiResponse<Page<Product>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Search Products
     * 
     * GET /api/v1/products/search?q=brake&page=0&size=10
     * 
     * Searches across product name, SKU, and barcode
     * 
     * Query Parameters:
     * - q: Search query string
     * - page, size, sort: Pagination params
     */
    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products by name, SKU, or barcode")
    public ResponseEntity<ApiResponse<Page<Product>>> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.searchProducts(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Get Product by ID
     * 
     * GET /api/v1/products/{id}
     * 
     * Path Variable:
     * - id: Product ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve a specific product")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    /**
     * Get Product by SKU
     * 
     * GET /api/v1/products/sku/{sku}
     * 
     * Path Variable:
     * - sku: Product SKU (Stock Keeping Unit)
     */
    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU", description = "Retrieve product by SKU")
    public ResponseEntity<ApiResponse<Product>> getProductBySku(@PathVariable String sku) {
        Product product = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    /**
     * Get Product by Barcode
     * 
     * GET /api/v1/products/barcode/{barcode}
     * 
     * Used for barcode/QR scanner integration
     * 
     * Path Variable:
     * - barcode: Product barcode/QR code
     */
    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "Get product by barcode", description = "Retrieve product by barcode (for scanning)")
    public ResponseEntity<ApiResponse<Product>> getProductByBarcode(@PathVariable String barcode) {
        Product product = productService.getProductByBarcode(barcode);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    /**
     * Create Product
     * 
     * POST /api/v1/products
     * 
     * Requires: ADMIN or MANAGER role
     * 
     * Request Body:
     * {
     *   "sku": "BRK-PADS-001",
     *   "name": "Ceramic Brake Pads",
     *   "description": "High-performance ceramic brake pads",
     *   "categoryId": 5,
     *   "brand": "Brembo",
     *   "model": "P06039",
     *   "barcode": "123456789012",
     *   "unitPrice": 89.99,
     *   "costPrice": 45.00,
     *   "unit": "Set",
     *   "reorderLevel": 10,
     *   "minimumStock": 5,
     *   "isActive": true
     * }
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create product", description = "Add new product to catalog (Admin/Manager only)")
    public ResponseEntity<ApiResponse<Product>> createProduct(@Valid @RequestBody ProductRequest request) {
        Product createdProduct = productService.createProduct(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", createdProduct));
    }

    /**
     * Update Product
     * 
     * PUT /api/v1/products/{id}
     * 
     * Requires: ADMIN or MANAGER role
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update product", description = "Update existing product (Admin/Manager only)")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        Product updatedProduct = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updatedProduct));
    }

    /**
     * Delete Product (Soft Delete)
     * 
     * DELETE /api/v1/products/{id}
     * 
     * Requires: ADMIN role only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product", description = "Soft delete a product (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }
}
