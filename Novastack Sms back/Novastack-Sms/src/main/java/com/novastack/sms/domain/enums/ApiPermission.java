package com.novastack.sms.domain.enums;

public enum ApiPermission {
    SMS_SEND,
    SMS_BULK,
    SMS_STATUS,
    SMS_HISTORY,
    /** Read organization wallet balance and transaction history from an integrating app. */
    WALLET_READ,
    /** Initiate and poll M-Pesa STK top-ups from an integrating app. */
    WALLET_TOPUP
}
