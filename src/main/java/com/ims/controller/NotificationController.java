package com.ims.controller;

import com.ims.dto.notification.NotificationDTO;
import com.ims.dto.notification.NotificationPreferenceDTO;
import com.ims.dto.notification.NotificationPreferenceRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.entity.User;
import com.ims.enums.NotificationPriority;
import com.ims.enums.NotificationType;
import com.ims.repository.UserRepository;
import com.ims.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "APIs for managing notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @PostMapping("/test-create")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create test notification", description = "FOR TESTING ONLY")
    public ResponseEntity<ApiResponse<Void>> createTestNotification(
            Authentication authentication,
            @RequestParam String title,
            @RequestParam String message,
            @RequestParam(defaultValue = "MEDIUM") String priority) {

        Long userId = getUserIdFromAuth(authentication);

        notificationService.createNotification(
                userId,
                NotificationType.USER_ACTION,
                NotificationPriority.valueOf(priority),
                title,
                message
        );

        return ResponseEntity.ok(ApiResponse.success("Test notification created", null));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user notifications", description = "Retrieve all notifications for the authenticated user")
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDTO> notifications = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread notifications", description = "Retrieve unread notifications for the authenticated user")
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getUnreadNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDTO> notifications = notificationService.getUnreadNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread count", description = "Get count of unread notifications")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        Long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark as read", description = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete notification", description = "Delete a notification")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", null));
    }

    // ==========================================
    // NOTIFICATION PREFERENCES
    // ==========================================

    @GetMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notification preferences", description = "Get email/SMS notification preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceDTO>> getPreferences(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getPreferences(userId)));
    }

    @PutMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update notification preferences", description = "Update email/SMS notification preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceDTO>> updatePreferences(
            Authentication authentication,
            @Valid @RequestBody NotificationPreferenceRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated",
                notificationService.updatePreferences(userId, request)));
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return user.getId();
    }
}
