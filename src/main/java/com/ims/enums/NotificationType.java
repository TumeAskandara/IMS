package com.ims.enums;

public enum NotificationType {
    LOW_STOCK,              // Product stock below reorder level
    OUT_OF_STOCK,           // Product completely out of stock
    OVERDUE_DEBT,           // Customer has overdue payment
    CREDIT_LIMIT_WARNING,   // Customer approaching credit limit
    TRANSFER_APPROVED,      // Stock transfer approved
    TRANSFER_REJECTED,      // Stock transfer rejected
    SALE_COMPLETED,         // Large sale completed
    SYSTEM_ALERT,           // General system alert
    USER_ACTION,            // User-triggered notification
    PURCHASE_ORDER_SUBMITTED,  // PO submitted for approval
    PURCHASE_ORDER_APPROVED,   // PO approved
    PURCHASE_ORDER_RECEIVED,   // Goods received against PO
    PURCHASE_ORDER_CANCELLED   // PO cancelled
}
