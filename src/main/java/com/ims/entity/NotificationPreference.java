package com.ims.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "email_enabled")
    @Builder.Default
    private Boolean emailEnabled = false;

    @Column(name = "sms_enabled")
    @Builder.Default
    private Boolean smsEnabled = false;

    @Column(length = 20)
    private String phone;

    // Per-type email toggles
    @Column(name = "low_stock_email")
    @Builder.Default
    private Boolean lowStockEmail = true;

    @Column(name = "low_stock_sms")
    @Builder.Default
    private Boolean lowStockSms = false;

    @Column(name = "overdue_debt_email")
    @Builder.Default
    private Boolean overdueDebtEmail = true;

    @Column(name = "overdue_debt_sms")
    @Builder.Default
    private Boolean overdueDebtSms = false;

    @Column(name = "purchase_order_email")
    @Builder.Default
    private Boolean purchaseOrderEmail = true;

    @Column(name = "purchase_order_sms")
    @Builder.Default
    private Boolean purchaseOrderSms = false;

    @Column(name = "transfer_email")
    @Builder.Default
    private Boolean transferEmail = true;

    @Column(name = "transfer_sms")
    @Builder.Default
    private Boolean transferSms = false;

    @Column(name = "sale_email")
    @Builder.Default
    private Boolean saleEmail = false;

    @Column(name = "sale_sms")
    @Builder.Default
    private Boolean saleSms = false;
}
