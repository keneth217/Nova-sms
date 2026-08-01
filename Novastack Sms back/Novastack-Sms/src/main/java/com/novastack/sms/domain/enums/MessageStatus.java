package com.novastack.sms.domain.enums;

/**
 * Customer-facing SMS lifecycle:
 * {@code PENDING} while queued/submitted to the provider and awaiting DLR,
 * then {@code DELIVERED} or {@code FAILED} from the delivery report.
 * {@code SCHEDULED} is only used before the send time arrives.
 */
public enum MessageStatus {
    PENDING,
    SCHEDULED,
    DELIVERED,
    FAILED
}
