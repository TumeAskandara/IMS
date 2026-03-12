package com.ims.service;

import com.ims.entity.Product;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BulkOperationsService {

    private final ProductRepository productRepository;

    public Map<String, Object> bulkUpdatePrices(List<Map<String, Object>> updates) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Map<String, Object> update : updates) {
            try {
                Long productId = ((Number) update.get("productId")).longValue();
                BigDecimal newUnitPrice = update.get("unitPrice") != null
                        ? new BigDecimal(update.get("unitPrice").toString()) : null;
                BigDecimal newCostPrice = update.get("costPrice") != null
                        ? new BigDecimal(update.get("costPrice").toString()) : null;

                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

                if (newUnitPrice != null) product.setUnitPrice(newUnitPrice);
                if (newCostPrice != null) product.setCostPrice(newCostPrice);
                productRepository.save(product);
                successCount++;
            } catch (Exception e) {
                errors.add("Product " + update.get("productId") + ": " + e.getMessage());
            }
        }

        return buildResult(updates.size(), successCount, errors);
    }

    public Map<String, Object> bulkUpdateStatus(List<Long> productIds, boolean isActive) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long productId : productIds) {
            try {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
                product.setIsActive(isActive);
                productRepository.save(product);
                successCount++;
            } catch (Exception e) {
                errors.add("Product " + productId + ": " + e.getMessage());
            }
        }

        return buildResult(productIds.size(), successCount, errors);
    }

    public Map<String, Object> bulkDelete(List<Long> productIds) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (Long productId : productIds) {
            try {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
                product.setIsDeleted(true);
                productRepository.save(product);
                successCount++;
            } catch (Exception e) {
                errors.add("Product " + productId + ": " + e.getMessage());
            }
        }

        return buildResult(productIds.size(), successCount, errors);
    }

    public Map<String, Object> bulkApplyPriceAdjustment(List<Long> productIds, BigDecimal percentageChange) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        BigDecimal multiplier = BigDecimal.ONE.add(percentageChange.divide(new BigDecimal("100")));

        for (Long productId : productIds) {
            try {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
                if (product.getUnitPrice() != null) {
                    product.setUnitPrice(product.getUnitPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP));
                }
                productRepository.save(product);
                successCount++;
            } catch (Exception e) {
                errors.add("Product " + productId + ": " + e.getMessage());
            }
        }

        return buildResult(productIds.size(), successCount, errors);
    }

    private Map<String, Object> buildResult(int total, int success, List<String> errors) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalProcessed", total);
        result.put("successCount", success);
        result.put("errorCount", errors.size());
        result.put("errors", errors);
        return result;
    }
}