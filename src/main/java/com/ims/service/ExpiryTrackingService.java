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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiryTrackingService {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Product> getProductsExpiringWithinDays(int days) {
        LocalDate now = LocalDate.now();
        return productRepository.findProductsExpiringBetween(now, now.plusDays(days));
    }

    @Transactional(readOnly = true)
    public List<Product> getExpiredProducts() {
        return productRepository.findProductsExpiringBefore(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Page<Product> getExpiredProductsPaged(Pageable pageable) {
        return productRepository.findExpiredProducts(LocalDate.now(), pageable);
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void checkExpiringProducts() {
        log.info("Running daily expiry check...");

        List<Product> expiringSoon = getProductsExpiringWithinDays(30);
        List<Product> expired = getExpiredProducts();

        List<User> admins = userRepository.findByRole(Role.ADMIN);
        List<User> managers = userRepository.findByRole(Role.MANAGER);

        if (!expired.isEmpty()) {
            String message = String.format("%d product(s) have EXPIRED. Immediate action required.", expired.size());
            for (User admin : admins) {
                notificationService.createNotification(admin.getId(),
                        NotificationType.EXPIRY_WARNING, NotificationPriority.CRITICAL,
                        "Expired Products Alert", message);
            }
            for (User manager : managers) {
                notificationService.createNotification(manager.getId(),
                        NotificationType.EXPIRY_WARNING, NotificationPriority.CRITICAL,
                        "Expired Products Alert", message);
            }
        }

        if (!expiringSoon.isEmpty()) {
            String message = String.format("%d product(s) expiring within 30 days.", expiringSoon.size());
            for (User admin : admins) {
                notificationService.createNotification(admin.getId(),
                        NotificationType.EXPIRY_WARNING, NotificationPriority.HIGH,
                        "Products Expiring Soon", message);
            }
        }

        log.info("Expiry check complete. Expired: {}, Expiring soon: {}", expired.size(), expiringSoon.size());
    }
}