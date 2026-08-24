package com.novastack.sms.security;

import com.novastack.sms.domain.enums.ApiPermission;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiPermissionFilterTest {

    @Test
    void smsPathsKeepExistingPermissions() {
        assertEquals(ApiPermission.SMS_SEND,
                ApiPermissionFilter.requiredPermission("POST", "/api/v1/sms/send"));
        assertEquals(ApiPermission.SMS_BULK,
                ApiPermissionFilter.requiredPermission("POST", "/api/v1/sms/bulk"));
        assertEquals(ApiPermission.SMS_BULK,
                ApiPermissionFilter.requiredPermission("POST", "/api/v1/sms/schedule"));
        assertEquals(ApiPermission.SMS_BULK,
                ApiPermissionFilter.requiredPermission("POST",
                        "/api/v1/sms/batches/11111111-1111-1111-1111-111111111111/resend-failed"));
        assertEquals(ApiPermission.SMS_SEND,
                ApiPermissionFilter.requiredPermission("POST",
                        "/api/v1/sms/11111111-1111-1111-1111-111111111111/resend"));
        assertEquals(ApiPermission.SMS_STATUS,
                ApiPermissionFilter.requiredPermission("GET",
                        "/api/v1/sms/batches/11111111-1111-1111-1111-111111111111"));
        assertEquals(ApiPermission.SMS_HISTORY,
                ApiPermissionFilter.requiredPermission("GET", "/api/v1/sms/history"));
        assertEquals(ApiPermission.SMS_HISTORY,
                ApiPermissionFilter.requiredPermission("GET", "/api/v1/sms"));
        assertEquals(ApiPermission.SMS_STATUS,
                ApiPermissionFilter.requiredPermission("GET", "/api/v1/sms/abc/status"));
        assertEquals(ApiPermission.SMS_STATUS,
                ApiPermissionFilter.requiredPermission("GET", "/api/v1/sms/abc"));
    }

    @Test
    void walletBalanceAndHistoryRequireRead() {
        assertEquals(ApiPermission.WALLET_READ,
                ApiPermissionFilter.requiredPermission("GET", "/api/v1/wallet/balance"));
        assertEquals(ApiPermission.WALLET_READ,
                ApiPermissionFilter.requiredPermission("GET", "/api/v1/wallet/transactions"));
    }

    @Test
    void walletTopupPathsRequireTopup() {
        assertEquals(ApiPermission.WALLET_TOPUP,
                ApiPermissionFilter.requiredPermission("POST", "/api/v1/wallet/topup"));
        assertEquals(ApiPermission.WALLET_TOPUP,
                ApiPermissionFilter.requiredPermission("GET",
                        "/api/v1/wallet/topup/11111111-1111-1111-1111-111111111111"));
        assertEquals(ApiPermission.WALLET_TOPUP,
                ApiPermissionFilter.requiredPermission("POST",
                        "/api/v1/wallet/topup/11111111-1111-1111-1111-111111111111/check"));
        assertEquals(ApiPermission.WALLET_TOPUP,
                ApiPermissionFilter.requiredPermission("POST", "/api/v1/wallet/topup/verify-receipt"));
    }

    @Test
    void mpesaStkPathsRequireMpesaPermissions() {
        assertEquals(ApiPermission.MPESA_STK_PUSH,
                ApiPermissionFilter.requiredPermission("POST", "/api/v1/mpesa/stkpush"));
        assertEquals(ApiPermission.MPESA_STK_PUSH,
                ApiPermissionFilter.requiredPermission("POST", "/api/v1/mpesa/checkout"));
        assertEquals(ApiPermission.MPESA_STATUS,
                ApiPermissionFilter.requiredPermission("GET",
                        "/api/v1/mpesa/transactions/11111111-1111-1111-1111-111111111111"));
        assertEquals(ApiPermission.MPESA_STATUS,
                ApiPermissionFilter.requiredPermission("GET",
                        "/api/v1/mpesa/transactions/11111111-1111-1111-1111-111111111111/status"));
        assertEquals(ApiPermission.MPESA_STATUS,
                ApiPermissionFilter.requiredPermission("GET",
                        "/api/v1/mpesa/checkout/11111111-1111-1111-1111-111111111111"));
        assertEquals(ApiPermission.MPESA_STATUS,
                ApiPermissionFilter.requiredPermission("GET",
                        "/api/v1/mpesa/checkout/11111111-1111-1111-1111-111111111111/status"));
        assertNull(ApiPermissionFilter.requiredPermission("POST", "/api/v1/mpesa/stk/callback"));
        assertNull(ApiPermissionFilter.requiredPermission("POST", "/api/v1/mpesa/c2b/confirmation"));
        assertNull(ApiPermissionFilter.requiredPermission("POST", "/api/v1/mpesa/c2b/validation"));
        assertNull(ApiPermissionFilter.requiredPermission("POST", "/api/v1/payments/c2b/confirmation"));
        assertNull(ApiPermissionFilter.requiredPermission("POST", "/api/v1/payments/transaction-status/result"));
        assertNull(ApiPermissionFilter.requiredPermission("POST", "/api/v1/payments/transaction-status/timeout"));
    }

    @Test
    void mpesaC2bClientPathsRequireC2bPermission() {
        assertEquals(ApiPermission.MPESA_C2B,
                ApiPermissionFilter.requiredPermission("GET", "/api/v1/mpesa/c2b"));
        assertEquals(ApiPermission.MPESA_C2B,
                ApiPermissionFilter.requiredPermission("GET", "/api/v1/mpesa/c2b/"));
        assertEquals(ApiPermission.MPESA_C2B,
                ApiPermissionFilter.requiredPermission("GET", "/api/v1/mpesa/c2b/transactions"));
        assertEquals(ApiPermission.MPESA_C2B,
                ApiPermissionFilter.requiredPermission("GET",
                        "/api/v1/mpesa/c2b/transactions/11111111-1111-1111-1111-111111111111"));
        assertEquals(ApiPermission.MPESA_C2B,
                ApiPermissionFilter.requiredPermission("POST", "/api/v1/mpesa/c2b/verify"));
    }

    @Test
    void otherResourcesRemainForbiddenForScopedKeys() {
        assertNull(ApiPermissionFilter.requiredPermission("GET", "/api/v1/contacts"));
        assertNull(ApiPermissionFilter.requiredPermission("POST", "/api/v1/auth/login"));
        assertNull(ApiPermissionFilter.requiredPermission("GET", "/api/v1/reports/dashboard"));
    }

    @Test
    void servletContextPrefixIsStripped() {
        assertEquals("/api/v1/wallet/balance",
                ApiPermissionFilter.normalizeApiPath("/novasms/api/v1/wallet/balance"));
        assertEquals(ApiPermission.WALLET_READ,
                ApiPermissionFilter.requiredPermission(
                        "GET",
                        ApiPermissionFilter.normalizeApiPath("/novasms/api/v1/wallet/balance")));
    }
}
