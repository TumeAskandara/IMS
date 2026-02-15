package com.ims.repository;

import com.ims.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
    
    Page<AuditLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
    
    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    Page<AuditLog> findByOrderByTimestampDesc(Pageable pageable);
}
