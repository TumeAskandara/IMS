package com.ims.enums;

public enum CustomerStatus {
    ACTIVE,      // Active customer
    INACTIVE,    // Inactive (no recent purchases)
    BLACKLISTED, // Banned from purchasing
    SUSPENDED    // Temporarily suspended (e.g., for overdue payments)
}
