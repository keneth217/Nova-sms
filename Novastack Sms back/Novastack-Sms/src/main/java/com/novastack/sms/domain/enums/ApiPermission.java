package com.novastack.sms.domain.enums;

public enum ApiPermission {
    SMS_SEND,
    SMS_BULK,
    SMS_STATUS,
    SMS_HISTORY,
    /** Read organization wallet balance and transaction history from an integrating app. */
    WALLET_READ,
    /** Initiate and poll M-Pesa STK top-ups from an integrating app. */
    WALLET_TOPUP,
    /** Initiate M-Pesa STK Push from POST /api/v1/mpesa/stkpush or /checkout. */
    MPESA_STK_PUSH,
    /** Read and refresh STK / checkout transaction status. */
    MPESA_STATUS,
    /** List and read Paybill C2B wallet credits; instructions and receipt verify. Not Daraja registration. */
    MPESA_C2B
}
