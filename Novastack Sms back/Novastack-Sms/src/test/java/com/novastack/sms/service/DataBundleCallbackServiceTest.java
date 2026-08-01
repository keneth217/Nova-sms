package com.novastack.sms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.DataBundleCallbackLog;
import com.novastack.sms.domain.entity.DataBundleTransaction;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.enums.BundleStatus;
import com.novastack.sms.domain.repository.DataBundleCallbackLogRepository;
import com.novastack.sms.domain.repository.DataBundleTransactionRepository;
import com.novastack.sms.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataBundleCallbackServiceTest {

    @Mock
    private DataBundleCallbackLogRepository callbackLogRepository;
    @Mock
    private DataBundleTransactionRepository transactionRepository;
    @Mock
    private WalletService walletService;

    private AppProperties appProperties;
    private DataBundleCallbackService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        service = new DataBundleCallbackService(
                callbackLogRepository,
                transactionRepository,
                walletService,
                appProperties,
                new ObjectMapper());
        lenient().when(callbackLogRepository.save(any(DataBundleCallbackLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void marksTransactionSuccessFromCallback() {
        DataBundleTransaction tx = pendingTx("NP-20260723-0001");
        when(transactionRepository.findByReference("NP-20260723-0001")).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(DataBundleTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.handleCallback(
                "{\"reference\":\"NP-20260723-0001\",\"status\":\"SUCCESS\",\"ResponseCode\":\"0\"}",
                null);

        assertEquals(0, result.get("ResultCode"));
        assertEquals(BundleStatus.SUCCESS, tx.getStatus());
        verify(walletService, never()).refund(any(), any(), anyString(), anyString());
    }

    @Test
    void refundsWalletOnFailedCallback() {
        DataBundleTransaction tx = pendingTx("NP-20260723-0002");
        tx.setWalletDebited(true);
        when(transactionRepository.findByReference("NP-20260723-0002")).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(DataBundleTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        service.handleCallback(
                "{\"reference\":\"NP-20260723-0002\",\"status\":\"FAILED\",\"ResponseCode\":\"1\",\"ResponseDescription\":\"Insufficient funds\"}",
                null);

        assertEquals(BundleStatus.FAILED, tx.getStatus());
        verify(walletService).refund(
                eq(tx.getOrganization().getId()),
                eq(tx.getAmount()),
                eq("BUNDLE-REFUND-NP-20260723-0002"),
                anyString());
    }

    @Test
    void rejectsInvalidCallbackTokenWhenConfigured() {
        appProperties.getDataBundles().setCallbackToken("secret-token");
        ApiException ex = assertThrows(ApiException.class, () -> service.handleCallback("{}", "wrong"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void ignoresAlreadyTerminalTransactions() {
        DataBundleTransaction tx = pendingTx("NP-DONE");
        tx.setStatus(BundleStatus.SUCCESS);
        service.applyRemoteStatus(tx, "FAILED", "1", "late failure");
        assertEquals(BundleStatus.SUCCESS, tx.getStatus());
        verify(transactionRepository, never()).save(any());
    }

    private static DataBundleTransaction pendingTx(String reference) {
        Organization org = Organization.builder().id(UUID.randomUUID()).build();
        return DataBundleTransaction.builder()
                .organization(org)
                .reference(reference)
                .phoneNumber("254712345678")
                .offerId("OFF123")
                .offerName("Daily 100MB")
                .amount(new BigDecimal("20.00"))
                .status(BundleStatus.PENDING)
                .walletDebited(false)
                .build();
    }
}
