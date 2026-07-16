package com.novastack.sms.domain.enums;

public enum OrganizationStatus {
    ACTIVE,
    SUSPENDED,
    PENDING,
    /** Event account past its one-week active window. */
    EXPIRED
}
