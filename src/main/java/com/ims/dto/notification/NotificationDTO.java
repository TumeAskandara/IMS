package com.ims.dto.notification;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String type;
    private String priority;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime readAt;
    private String actionUrl;
    private String relatedEntity;
    private Long relatedEntityId;
    private LocalDateTime createdAt;
}
