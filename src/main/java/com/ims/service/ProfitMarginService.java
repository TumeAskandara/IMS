package com.ims.service;

import com.ims.entity.Product;
import com.ims.entity.User;
import com.ims.enums.NotificationPriority;
import com.ims.enums.NotificationType;
import com.ims.enums.Role;
import com.ims.repository.ProductRepository;
import com.ims.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfitMarginService {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private static final BigDecimal LOW_MARGIN_THRESHOLD = new BigDecimal("10"); // 10%
    private static final BigDecimal NEGATIVE_MARGIN_THRESHOLD = BigDecimal.ZERO;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProductsWithLowMargins() {
        List<Product> products = productRepository.findByIsDeletedFalse(
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        return products.stream()
                .filter(p -> p.getCostPrice() != null && p.getUnitPrice() != null
                        && p.getCostPrice().compareTo(BigDecimal.ZERO) > 0)
                .map(p -> {
                    BigDecimal margin = calculateMarginPercentage(p);
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("productId", p.getId());
                    result.put("sku", p.getSku());
                    result.put("name", p.getName());
                    result.put("costPrice", p.getCostPrice());
                    result.put("unitPrice", p.getUnitPrice());
                    result.put("marginPercentage", margin);
                    result.put("marginStatus", margin.compareTo(NEGATIVE_MARGIN_THRESHOLD) < 0 ? "NEGATIVE"
                            : margin.compareTo(LOW_MARGIN_THRESHOLD) < 0 ? "LOW" : "HEALTHY");
                    return result;
                })
                .filter(m -> {
                    BigDecimal margin = (BigDecimal) m.get("marginPercentage");
                    return margin.compareTo(LOW_MARGIN_THRESHOLD) < 0;
                })
                .sorted(Comparator.comparing(m -> (BigDecimal) m.get("marginPercentage")))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkProductMargin(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new com.ims.exception.ResourceNotFoundException("Product", "id", productId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productId", product.getId());
        result.put("name", product.getName());

        if (product.getCostPrice() == null || product.getUnitPrice() == null) {
            result.put("warning", "Cost price or unit price not set");
            return result;
        }

        BigDecimal margin = calculateMarginPercentage(product);
        BigDecimal profit = product.getUnitPrice().subtract(product.getCostPrice());

        result.put("costPrice", product.getCostPrice());
        result.put("unitPrice", product.getUnitPrice());
        result.put("profitPerUnit", profit);
        result.put("marginPercentage", margin);

        if (margin.compareTo(NEGATIVE_MARGIN_THRESHOLD) < 0) {
            result.put("warning", "SELLING BELOW COST - Negative margin!");
        } else if (margin.compareTo(LOW_MARGIN_THRESHOLD) < 0) {
            result.put("warning", "Low profit margin - below " + LOW_MARGIN_THRESHOLD + "%");
        } else {
            result.put("status", "Healthy margin");
        }

        return result;
    }

    public void checkMarginOnSale(Long productId, BigDecimal sellingPrice) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || product.getCostPrice() == null) return;

        BigDecimal margin = sellingPrice.subtract(product.getCostPrice())
                .divide(sellingPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        if (margin.compareTo(NEGATIVE_MARGIN_THRESHOLD) < 0) {
            List<User> admins = userRepository.findByRole(Role.ADMIN);
            for (User admin : admins) {
                notificationService.createNotification(admin.getId(),
                        NotificationType.PROFIT_MARGIN_WARNING, NotificationPriority.CRITICAL,
                        "Selling Below Cost!",
                        String.format("Product '%s' (SKU: %s) sold at %s below cost price %s. Margin: %.1f%%",
                                product.getName(), product.getSku(),
                                sellingPrice.toPlainString(), product.getCostPrice().toPlainString(),
                                margin.doubleValue()));
            }
        } else if (margin.compareTo(LOW_MARGIN_THRESHOLD) < 0) {
            List<User> managers = userRepository.findByRole(Role.MANAGER);
            for (User manager : managers) {
                notificationService.createNotification(manager.getId(),
                        NotificationType.PROFIT_MARGIN_WARNING, NotificationPriority.HIGH,
                        "Low Profit Margin Warning",
                        String.format("Product '%s' (SKU: %s) has low margin %.1f%%. Selling: %s, Cost: %s",
                                product.getName(), product.getSku(), margin.doubleValue(),
                                sellingPrice.toPlainString(), product.getCostPrice().toPlainString()));
            }
        }
    }

    private BigDecimal calculateMarginPercentage(Product product) {
        if (product.getUnitPrice().compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return product.getUnitPrice().subtract(product.getCostPrice())
                .divide(product.getUnitPrice(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}