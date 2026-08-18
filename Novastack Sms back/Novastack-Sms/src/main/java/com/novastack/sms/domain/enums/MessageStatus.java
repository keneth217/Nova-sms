package com.novastack.sms.domain.enums;

public enum MessageStatus {
    PENDING,
    QUEUED,
    PROCESSING,
    ACCEPTED,
    SENT,
    SCHEDULED,
    DELIVERED,
    FAILED,
    REJECTED,
    CANCELLED;

    public boolean isTerminal() {
        return this == DELIVERED || this == FAILED || this == REJECTED || this == CANCELLED;
    }

    public boolean isInFlight() {
        return this == PENDING || this == QUEUED || this == PROCESSING || this == ACCEPTED || this == SENT;
    }

    public boolean isBillableFailure() {
        return this == FAILED || this == REJECTED || this == CANCELLED;
    }
}
