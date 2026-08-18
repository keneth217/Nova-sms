package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.entity.Wallet;
import com.novastack.sms.domain.entity.WalletTransaction;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.domain.repository.WalletRepository;
import com.novastack.sms.domain.repository.WalletTransactionRepository;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.mpesa.MpesaDarajaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private MpesaDarajaClient mpesaDarajaClient;
    @Mock
    private BillingSettingsService billingSettingsService;
    @Mock
    private SmsBillingCalculator smsBillingCalculator;

    private WalletService walletService;
    private Organization organization;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(
                walletRepository,
                walletTransactionRepository,
                organizationRepository,
                new AppProperties(),
                mpesaDarajaClient,
                billingSettingsService,
                smsBillingCalculator);
        organization = Organization.builder()
                .id(UUID.randomUUID())
                .name("Acme")
                .smsCost(new BigDecimal("1.00"))
                .build();
        wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .balance(new BigDecimal("10.00"))
                .currency("KES")
                .build();
    }

    @Test
    void sufficientBalanceDebitsWallet() {
        when(walletRepository.findByOrganizationIdForUpdate(organization.getId())).thenReturn(Optional.of(wallet));

        walletService.debitForSms(organization.getId(), new BigDecimal("1.50"), "SMS-1", "test");

        assertEquals(new BigDecimal("8.50"), wallet.getBalance());
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void insufficientBalanceIsRejected() {
        when(walletRepository.findByOrganizationIdForUpdate(organization.getId())).thenReturn(Optional.of(wallet));

        ApiException ex = assertThrows(ApiException.class,
                () -> walletService.debitForSms(organization.getId(), new BigDecimal("11.00"), "SMS-2", "test"));

        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatus());
        assertEquals("Insufficient wallet balance", ex.getMessage());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) ex.getData();
        assertEquals(0, ((BigDecimal) data.get("required")).compareTo(new BigDecimal("11.00")));
        assertEquals(0, ((BigDecimal) data.get("available")).compareTo(new BigDecimal("10.00")));
        assertEquals("KES", data.get("currency"));
        assertEquals(new BigDecimal("10.00"), wallet.getBalance());
        verify(walletTransactionRepository, never()).save(any());
    }

    @Test
    void exactBalanceIsAccepted() {
        when(walletRepository.findByOrganizationIdForUpdate(organization.getId())).thenReturn(Optional.of(wallet));

        walletService.debitForSms(organization.getId(), new BigDecimal("10.00"), "SMS-3", "test");

        assertEquals(new BigDecimal("10.00").subtract(new BigDecimal("10.00")), wallet.getBalance());
        assertEquals(0, wallet.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void providerFailureRefundRestoresBalance() {
        when(walletRepository.findByOrganizationIdForUpdate(organization.getId())).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.findByReference("REFUND-1")).thenReturn(Optional.empty());

        walletService.refund(organization.getId(), new BigDecimal("1.50"), "REFUND-1", "failed send");

        assertEquals(new BigDecimal("11.50"), wallet.getBalance());
        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(captor.capture());
        assertEquals(new BigDecimal("1.50"), captor.getValue().getAmount());
    }

    @Test
    void secondDebitFailsWhenBalanceWouldGoNegative() {
        when(walletRepository.findByOrganizationIdForUpdate(organization.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.debitForSms(organization.getId(), new BigDecimal("6.00"), "SMS-A", "first");
        ApiException ex = assertThrows(ApiException.class,
                () -> walletService.debitForSms(organization.getId(), new BigDecimal("6.00"), "SMS-B", "second"));

        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatus());
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("4.00")));
        verify(walletTransactionRepository, times(1)).save(any());
    }

    @Test
    void duplicateDebitReferenceIsNotChargedTwice() {
        when(walletTransactionRepository.findByReference("SMS-DUP")).thenReturn(Optional.of(new WalletTransaction()));

        walletService.debitForSms(organization.getId(), new BigDecimal("1.00"), "SMS-DUP", "retry");

        assertEquals(new BigDecimal("10.00"), wallet.getBalance());
        verify(walletRepository, never()).findByOrganizationIdForUpdate(any());
        verify(walletTransactionRepository, never()).save(any());
    }

    @Test
    void duplicateRefundReferenceIsNotCreditedTwice() {
        when(walletTransactionRepository.findByReference("REFUND-1")).thenReturn(Optional.of(new WalletTransaction()));

        walletService.refund(organization.getId(), new BigDecimal("1.50"), "REFUND-1", "failed send");

        assertEquals(new BigDecimal("10.00"), wallet.getBalance());
        verify(walletRepository, never()).findByOrganizationIdForUpdate(any());
    }
}
