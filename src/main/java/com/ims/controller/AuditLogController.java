package com.ims.controller;

import com.ims.dto.response.ApiResponse;
import com.ims.entity.AuditLog;
import com.ims.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Log", description = "View audit trail / activity log")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @Operation(summary = "Get all audit logs", description = "Paginated list of all audit entries")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<AuditLog> logs = auditLogRepository.findByOrderByTimestampDesc(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get audit logs for specific entity")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getEntityAuditLogs(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get audit logs by user")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getUserAuditLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<AuditLog> logs = auditLogRepository.findByUserIdOrderByTimestampDesc(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get audit logs by date range")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogsByDateRange(
            @RequestParam String from,
            @RequestParam String to) {
        LocalDateTime fromDate = LocalDateTime.parse(from);
        LocalDateTime toDate = LocalDateTime.parse(to);
        List<AuditLog> logs = auditLogRepository.findByTimestampBetween(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}