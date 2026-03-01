package com.ims.service;

import com.ims.dto.notification.NotificationDTO;
import com.ims.dto.notification.NotificationPreferenceDTO;
import com.ims.dto.notification.NotificationPreferenceRequest;
import com.ims.entity.Notification;
import com.ims.entity.NotificationPreference;
import com.ims.entity.User;
import com.ims.enums.NotificationPriority;
import com.ims.enums.NotificationType;
import com.ims.enums.Role;
import com.ims.exception.ResourceNotFoundException;
import com.ims.repository.NotificationPreferenceRepository;
import com.ims.repository.NotificationRepository;
import com.ims.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired(required = false)
    private SmsService smsService;

    public void createNotification(Long userId, NotificationType type, NotificationPriority priority,
                                  String title, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = Notification.builder()
                .type(type)
                .priority(priority)
                .title(title)
                .message(message)
                .user(user)
                .build();

        notificationRepository.save(notification);
        log.info("Created {} notification for user {}: {}", priority, userId, title);

        dispatchExternalNotifications(user, type, title, message);
    }

    public void createNotificationForAllAdmins(NotificationType type, NotificationPriority priority,
                                             String title, String message) {
        List<User> adminsAndManagers = new java.util.ArrayList<>(userRepository.findByRole(Role.ADMIN));
        adminsAndManagers.addAll(userRepository.findByRole(Role.MANAGER));

        for (User user : adminsAndManagers) {
            Notification notification = Notification.builder()
                    .type(type)
                    .priority(priority)
                    .title(title)
                    .message(message)
                    .user(user)
                    .build();
            notificationRepository.save(notification);

            dispatchExternalNotifications(user, type, title, message);
        }
        log.info("Created {} notification for {} admin/manager users: {}", priority, adminsAndManagers.size(), title);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getUserNotifications(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getUnreadNotifications(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return notificationRepository.countUnreadByUser(user);
    }

    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.markAsRead();
        notificationRepository.save(notification);
    }

    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<Notification> unread = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(
                user, Pageable.unpaged());

        unread.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unread);
    }

    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notificationRepository.delete(notification);
    }

    // ==========================================
    // NOTIFICATION PREFERENCES
    // ==========================================

    @Transactional(readOnly = true)
    public NotificationPreferenceDTO getPreferences(Long userId) {
        NotificationPreference pref = notificationPreferenceRepository.findByUserId(userId)
                .orElse(null);
        if (pref == null) {
            return NotificationPreferenceDTO.builder()
                    .userId(userId)
                    .emailEnabled(false)
                    .smsEnabled(false)
                    .lowStockEmail(true)
                    .lowStockSms(false)
                    .overdueDebtEmail(true)
                    .overdueDebtSms(false)
                    .purchaseOrderEmail(true)
                    .purchaseOrderSms(false)
                    .transferEmail(true)
                    .transferSms(false)
                    .saleEmail(false)
                    .saleSms(false)
                    .build();
        }
        return mapPrefToDTO(pref);
    }

    public NotificationPreferenceDTO updatePreferences(Long userId, NotificationPreferenceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        NotificationPreference pref = notificationPreferenceRepository.findByUserId(userId)
                .orElse(NotificationPreference.builder().user(user).build());

        if (request.getEmailEnabled() != null) pref.setEmailEnabled(request.getEmailEnabled());
        if (request.getSmsEnabled() != null) pref.setSmsEnabled(request.getSmsEnabled());
        if (request.getPhone() != null) pref.setPhone(request.getPhone());
        if (request.getLowStockEmail() != null) pref.setLowStockEmail(request.getLowStockEmail());
        if (request.getLowStockSms() != null) pref.setLowStockSms(request.getLowStockSms());
        if (request.getOverdueDebtEmail() != null) pref.setOverdueDebtEmail(request.getOverdueDebtEmail());
        if (request.getOverdueDebtSms() != null) pref.setOverdueDebtSms(request.getOverdueDebtSms());
        if (request.getPurchaseOrderEmail() != null) pref.setPurchaseOrderEmail(request.getPurchaseOrderEmail());
        if (request.getPurchaseOrderSms() != null) pref.setPurchaseOrderSms(request.getPurchaseOrderSms());
        if (request.getTransferEmail() != null) pref.setTransferEmail(request.getTransferEmail());
        if (request.getTransferSms() != null) pref.setTransferSms(request.getTransferSms());
        if (request.getSaleEmail() != null) pref.setSaleEmail(request.getSaleEmail());
        if (request.getSaleSms() != null) pref.setSaleSms(request.getSaleSms());

        pref = notificationPreferenceRepository.save(pref);
        log.info("Updated notification preferences for user {}", userId);
        return mapPrefToDTO(pref);
    }

    // ==========================================
    // EXTERNAL DISPATCH
    // ==========================================

    private void dispatchExternalNotifications(User user, NotificationType type, String title, String message) {
        try {
            NotificationPreference pref = notificationPreferenceRepository.findByUserId(user.getId())
                    .orElse(null);
            if (pref == null) return;

            boolean shouldEmail = pref.getEmailEnabled() && shouldSendEmail(pref, type);
            boolean shouldSms = pref.getSmsEnabled() && shouldSendSms(pref, type);

            if (shouldEmail && emailService != null && user.getEmail() != null) {
                emailService.sendAlertEmail(user.getEmail(), title, message);
            }

            if (shouldSms && smsService != null && pref.getPhone() != null) {
                smsService.sendSms(pref.getPhone(), "[IMS] " + title + ": " + message);
            }
        } catch (Exception e) {
            log.error("Failed to dispatch external notifications for user {}: {}",
                    user.getId(), e.getMessage());
        }
    }

    private boolean shouldSendEmail(NotificationPreference pref, NotificationType type) {
        return switch (type) {
            case LOW_STOCK, OUT_OF_STOCK -> pref.getLowStockEmail();
            case OVERDUE_DEBT, CREDIT_LIMIT_WARNING -> pref.getOverdueDebtEmail();
            case PURCHASE_ORDER_SUBMITTED, PURCHASE_ORDER_APPROVED,
                 PURCHASE_ORDER_RECEIVED, PURCHASE_ORDER_CANCELLED -> pref.getPurchaseOrderEmail();
            case TRANSFER_APPROVED, TRANSFER_REJECTED -> pref.getTransferEmail();
            case SALE_COMPLETED -> pref.getSaleEmail();
            case SYSTEM_ALERT, USER_ACTION -> true;
        };
    }

    private boolean shouldSendSms(NotificationPreference pref, NotificationType type) {
        return switch (type) {
            case LOW_STOCK, OUT_OF_STOCK -> pref.getLowStockSms();
            case OVERDUE_DEBT, CREDIT_LIMIT_WARNING -> pref.getOverdueDebtSms();
            case PURCHASE_ORDER_SUBMITTED, PURCHASE_ORDER_APPROVED,
                 PURCHASE_ORDER_RECEIVED, PURCHASE_ORDER_CANCELLED -> pref.getPurchaseOrderSms();
            case TRANSFER_APPROVED, TRANSFER_REJECTED -> pref.getTransferSms();
            case SALE_COMPLETED -> pref.getSaleSms();
            case SYSTEM_ALERT, USER_ACTION -> true;
        };
    }

    // ==========================================
    // MAPPERS
    // ==========================================

    private NotificationDTO mapToDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .priority(notification.getPriority().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .actionUrl(notification.getActionUrl())
                .relatedEntity(notification.getRelatedEntity())
                .relatedEntityId(notification.getRelatedEntityId())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private NotificationPreferenceDTO mapPrefToDTO(NotificationPreference pref) {
        return NotificationPreferenceDTO.builder()
                .id(pref.getId())
                .userId(pref.getUser().getId())
                .emailEnabled(pref.getEmailEnabled())
                .smsEnabled(pref.getSmsEnabled())
                .phone(pref.getPhone())
                .lowStockEmail(pref.getLowStockEmail())
                .lowStockSms(pref.getLowStockSms())
                .overdueDebtEmail(pref.getOverdueDebtEmail())
                .overdueDebtSms(pref.getOverdueDebtSms())
                .purchaseOrderEmail(pref.getPurchaseOrderEmail())
                .purchaseOrderSms(pref.getPurchaseOrderSms())
                .transferEmail(pref.getTransferEmail())
                .transferSms(pref.getTransferSms())
                .saleEmail(pref.getSaleEmail())
                .saleSms(pref.getSaleSms())
                .build();
    }
}
