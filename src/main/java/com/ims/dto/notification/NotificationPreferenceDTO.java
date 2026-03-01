package com.ims.dto.notification;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceDTO {
    private Long id;
    private Long userId;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private String phone;
    private Boolean lowStockEmail;
    private Boolean lowStockSms;
    private Boolean overdueDebtEmail;
    private Boolean overdueDebtSms;
    private Boolean purchaseOrderEmail;
    private Boolean purchaseOrderSms;
    private Boolean transferEmail;
    private Boolean transferSms;
    private Boolean saleEmail;
    private Boolean saleSms;
}
