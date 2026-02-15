package com.ims.service;

import com.ims.dto.request.ProductRequest;
import com.ims.entity.Category;
import com.ims.entity.Product;
import com.ims.exception.BadRequestException;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.CategoryRepository;
import com.ims.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findByIsDeletedFalse(pageable);
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    @Transactional(readOnly = true)
    public Product getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
    }

    @Transactional(readOnly = true)
    public Product getProductByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "barcode", barcode));
    }

    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String search, Pageable pageable) {
        return productRepository.searchProducts(search, pageable);
    }

    @Transactional
    public Product createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Product with SKU " + request.getSku() + " already exists");
        }

        if (request.getBarcode() != null && productRepository.existsByBarcode(request.getBarcode())) {
            throw new BadRequestException("Product with barcode " + request.getBarcode() + " already exists");
        }

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .brand(request.getBrand())
                .model(request.getModel())
                .barcode(request.getBarcode())
                .unitPrice(request.getUnitPrice())
                .costPrice(request.getCostPrice())
                .unit(request.getUnit())
                .reorderLevel(request.getReorderLevel())
                .minimumStock(request.getMinimumStock())
                .notes(request.getNotes())
                .imageUrl(request.getImageUrl())
                .isActive(request.getIsActive())
                .build();

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        }

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, ProductRequest request) {
        Product product = getProductById(id);

        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Product with SKU " + request.getSku() + " already exists");
        }

        if (request.getBarcode() != null && !request.getBarcode().equals(product.getBarcode()) && 
            productRepository.existsByBarcode(request.getBarcode())) {
            throw new BadRequestException("Product with barcode " + request.getBarcode() + " already exists");
        }

        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setModel(request.getModel());
        product.setBarcode(request.getBarcode());
        product.setUnitPrice(request.getUnitPrice());
        product.setCostPrice(request.getCostPrice());
        product.setUnit(request.getUnit());
        product.setReorderLevel(request.getReorderLevel());
        product.setMinimumStock(request.getMinimumStock());
        product.setNotes(request.getNotes());
        product.setImageUrl(request.getImageUrl());
        product.setIsActive(request.getIsActive());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        }

        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setIsDeleted(true);
        productRepository.save(product);
    }
}
