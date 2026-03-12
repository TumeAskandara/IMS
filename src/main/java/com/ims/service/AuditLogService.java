package com.ims.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.entity.AuditLog;
import com.ims.entity.User;
import com.ims.repository.AuditLogRepository;
import com.ims.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreate(String entityType, Long entityId, Object newEntity) {
        saveLog(entityType, entityId, "CREATE", null, toJson(newEntity));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(String entityType, Long entityId, Object oldValues, Object newValues) {
        saveLog(entityType, entityId, "UPDATE", toJson(oldValues), toJson(newValues));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDelete(String entityType, Long entityId, Object oldEntity) {
        saveLog(entityType, entityId, "DELETE", toJson(oldEntity), null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String entityType, Long entityId, String action, String details) {
        saveLog(entityType, entityId, action, null, details);
    }

    private void saveLog(String entityType, Long entityId, String action, String oldValues, String newValues) {
        try {
            User currentUser = getCurrentUser();
            String ipAddress = getClientIpAddress();

            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .user(currentUser)
                    .timestamp(LocalDateTime.now())
                    .oldValues(oldValues)
                    .newValues(newValues)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log: {} {} {} by {}", action, entityType, entityId,
                    currentUser != null ? currentUser.getUsername() : "SYSTEM");
        } catch (Exception e) {
            log.error("Failed to save audit log for {} {} {}: {}", action, entityType, entityId, e.getMessage());
        }
    }

    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                return userRepository.findByUsername(auth.getName()).orElse(null);
            }
        } catch (Exception e) {
            log.debug("Could not resolve current user for audit log");
        }
        return null;
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not resolve IP address for audit log");
        }
        return null;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String) return (String) obj;
        try {
            if (obj instanceof Map) {
                return objectMapper.writeValueAsString(obj);
            }
            // For entities, extract key fields to avoid circular references
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.convertValue(obj, Map.class));
        } catch (Exception e) {
            return obj.toString();
        }
    }
}