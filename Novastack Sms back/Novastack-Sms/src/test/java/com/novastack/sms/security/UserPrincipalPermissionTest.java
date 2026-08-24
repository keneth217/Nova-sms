package com.novastack.sms.security;

import com.novastack.sms.domain.entity.ApiClient;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.enums.ApiPermission;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPrincipalPermissionTest {

    @Test
    void walletTopupImpliesMpesaStkAndStatus() {
        UserPrincipal principal = principalWith(ApiPermission.WALLET_TOPUP);

        assertTrue(principal.hasPermission(ApiPermission.WALLET_TOPUP));
        assertTrue(principal.hasPermission(ApiPermission.MPESA_STK_PUSH));
        assertTrue(principal.hasPermission(ApiPermission.MPESA_STATUS));
        assertTrue(principal.hasPermission(ApiPermission.MPESA_C2B));
        assertFalse(principal.hasPermission(ApiPermission.SMS_SEND));
    }

    @Test
    void mpesaStkPushImpliesStatusButNotWalletTopup() {
        UserPrincipal principal = principalWith(ApiPermission.MPESA_STK_PUSH);

        assertTrue(principal.hasPermission(ApiPermission.MPESA_STK_PUSH));
        assertTrue(principal.hasPermission(ApiPermission.MPESA_STATUS));
        assertFalse(principal.hasPermission(ApiPermission.WALLET_TOPUP));
    }

    @Test
    void mpesaStatusDoesNotAllowStkPush() {
        UserPrincipal principal = principalWith(ApiPermission.MPESA_STATUS);

        assertTrue(principal.hasPermission(ApiPermission.MPESA_STATUS));
        assertFalse(principal.hasPermission(ApiPermission.MPESA_STK_PUSH));
        assertFalse(principal.hasPermission(ApiPermission.MPESA_C2B));
    }

    @Test
    void walletReadImpliesC2bButNotStk() {
        UserPrincipal principal = principalWith(ApiPermission.WALLET_READ);

        assertTrue(principal.hasPermission(ApiPermission.WALLET_READ));
        assertTrue(principal.hasPermission(ApiPermission.MPESA_C2B));
        assertFalse(principal.hasPermission(ApiPermission.MPESA_STK_PUSH));
        assertFalse(principal.hasPermission(ApiPermission.WALLET_TOPUP));
    }

    private static UserPrincipal principalWith(ApiPermission permission) {
        Organization org = Organization.builder().id(UUID.randomUUID()).name("Acme").build();
        ApiClient client = ApiClient.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .name("Acme API")
                .clientCode("ACME")
                .apiKeyHash("hash")
                .apiKeyPrefix("nova_live_test")
                .permissions(EnumSet.of(permission))
                .build();
        return UserPrincipal.fromApiClient(client);
    }
}
