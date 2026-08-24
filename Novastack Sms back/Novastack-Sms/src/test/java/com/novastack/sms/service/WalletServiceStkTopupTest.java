package com.novastack.sms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.MpesaC2bInbound;
import com.novastack.sms.domain.entity.MpesaTransactionStatusQuery;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.entity.PaybillCollection;
import com.novastack.sms.domain.entity.Wallet;
import com.novastack.sms.domain.entity.WalletTransaction;
import com.novastack.sms.domain.enums.PaymentMethod;
import com.novastack.sms.domain.enums.TopupStatus;
import com.novastack.sms.domain.enums.TransactionStatusQueryState;
import com.novastack.sms.domain.enums.WalletTransactionType;
import com.novastack.sms.domain.repository.MpesaTransactionStatusQueryRepository;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.domain.repository.WalletRepository;
import com.novastack.sms.domain.repository.WalletTransactionRepository;
import com.novastack.sms.dto.response.StkPushResponse;
import com.novastack.sms.dto.response.WalletBalanceResponse;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.mpesa.MpesaDarajaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceStkTopupTest {

    private static final String CHECKOUT = "ws_CO_19082026104512345";
    private static final String RECEIPT = "UHJA53YW7O";
    private static final String C2B_RECEIPT = "ABC123XYZ";
    private static final String BILL_REF = "NOVATEST";
    private static final String HASHED_MSISDN =
            "94c392c311d522da950619227b3361752a42042db7e1e699b26e628305c68a88";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    @Mock
    private OrgNotificationService orgNotificationService;
    @Mock
    private PaybillCollectionService paybillCollectionService;
    @Mock
    private C2bInboundService c2bInboundService;
    @Mock
    private MpesaTransactionStatusQueryRepository transactionStatusQueryRepository;

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
                smsBillingCalculator,
                orgNotificationService,
                paybillCollectionService,
                c2bInboundService,
                transactionStatusQueryRepository);
        organization = Organization.builder()
                .id(UUID.randomUUID())
                .name("Acme")
                .mpesaAccountRef(BILL_REF)
                .build();
        wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .balance(new BigDecimal("10.00"))
                .currency("KES")
                .build();
        lenient().when(organizationRepository.findByMpesaAccountRefIgnoreCase(any()))
                .thenReturn(Optional.empty());
        lenient().when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(paybillCollectionService.isCollectionAccount(any())).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (!(arg instanceof String ref) || ref.isBlank()) {
                return false;
            }
            String normalized = ref.trim().toUpperCase();
            return "SHEILA".equals(normalized) || "KENETH".equals(normalized);
        });
        lenient().when(paybillCollectionService.existsByReceipt(any())).thenReturn(false);
        lenient().when(paybillCollectionService.findByReceipt(any())).thenReturn(Optional.empty());
        lenient().when(walletTransactionRepository.findByMpesaReceiptIgnoreCase(any())).thenReturn(Optional.empty());
        lenient().when(walletTransactionRepository.findByReferenceIgnoreCase(any())).thenReturn(Optional.empty());
        lenient().when(c2bInboundService.findByReceipt(any())).thenReturn(Optional.empty());
    }

    @Test
    void paybillAccountIsNovaPlusFourCharacters() {
        UUID mwalimuId = UUID.fromString("c727c94f-61fa-47a6-bfa0-b650b48c303c");
        assertEquals("NOVAC727", walletService.buildAccountReference(mwalimuId));
        assertEquals(8, walletService.buildAccountReference(mwalimuId).length());
    }

    @Test
    void longPaybillAccountIsShortenedToEightCharacters() {
        organization.setMpesaAccountRef("NOVAC727C94F");
        Organization saved = walletService.ensureMpesaAccountRef(organization);
        assertEquals("NOVAC727", saved.getMpesaAccountRef());
        verify(organizationRepository).save(organization);
    }

    @Test
    void balanceIncludesPaybillAccountAndCustomerSmsCost() {
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        when(walletRepository.findByOrganizationId(organization.getId())).thenReturn(Optional.of(wallet));
        when(billingSettingsService.customerPrice()).thenReturn(new BigDecimal("1.00"));
        when(smsBillingCalculator.availableSms(wallet.getBalance())).thenReturn(10L);

        WalletBalanceResponse balance = walletService.getBalance(organization.getId());

        assertEquals(new AppProperties().getMpesa().getShortcode(), balance.getPaybill());
        assertEquals(BILL_REF, balance.getAccountNumber());
        assertEquals("Novastack SMS", balance.getBusinessName());
        assertEquals(0, new BigDecimal("1.00").compareTo(balance.getSmsCost()));
        assertEquals(10L, balance.getAvailableSms());
        assertEquals(0, new BigDecimal("10.00").compareTo(balance.getBalance()));
    }

    @Test
    void processingQueryThenSuccessfulCallbackCreditsOnce() throws Exception {
        WalletTransaction tx = pendingTopup();
        stubLookup(tx);
        when(mpesaDarajaClient.queryStkStatus(CHECKOUT)).thenReturn(processingQuery());

        StkPushResponse pending = walletService.checkTopUpTransaction(organization.getId(), tx.getId());

        assertEquals(TopupStatus.PENDING, pending.getStatus());
        assertFalse(pending.isCallbackReceived());
        assertFalse(pending.isWalletCredited());
        verify(walletRepository, never()).findByOrganizationIdForUpdate(any());

        stubCredit();
        walletService.handleStkCallback(successCallback());

        assertEquals(TopupStatus.COMPLETED, tx.getTopupStatus());
        assertTrue(tx.isCallbackReceived());
        assertTrue(tx.isWalletCredited());
        assertEquals(RECEIPT, tx.getMpesaReceipt());
        assertEquals(0, new BigDecimal("11.00").compareTo(wallet.getBalance()));
        verify(walletRepository, times(1)).save(wallet);
        verify(orgNotificationService).notifyTopUpSuccess(any(), any(), any(), eq(RECEIPT));
        verify(orgNotificationService).notifyPlatformOwnerTopUp(any(), any(), any(), eq(RECEIPT));
    }

    @Test
    void duplicateSuccessfulCallbackDoesNotCreditTwice() throws Exception {
        WalletTransaction tx = pendingTopup();
        stubLookup(tx);
        when(mpesaDarajaClient.queryStkStatus(CHECKOUT)).thenReturn(processingQuery());
        walletService.checkTopUpTransaction(organization.getId(), tx.getId());

        stubCredit();
        JsonNode callback = successCallback();
        walletService.handleStkCallback(callback);
        walletService.handleStkCallback(callback);

        assertEquals(TopupStatus.COMPLETED, tx.getTopupStatus());
        assertTrue(tx.isWalletCredited());
        assertEquals(0, new BigDecimal("11.00").compareTo(wallet.getBalance()));
        verify(walletRepository, times(1)).save(wallet);
        verify(orgNotificationService, times(1)).notifyTopUpSuccess(any(), any(), any(), any());
        verify(orgNotificationService, times(1)).notifyPlatformOwnerTopUp(any(), any(), any(), any());
    }

    @Test
    void processingQueryThenFailedCallbackDoesNotCredit() throws Exception {
        WalletTransaction tx = pendingTopup();
        stubLookup(tx);
        when(mpesaDarajaClient.queryStkStatus(CHECKOUT)).thenReturn(processingQuery());
        walletService.checkTopUpTransaction(organization.getId(), tx.getId());

        walletService.handleStkCallback(failedCallback(1032, "Request cancelled by user"));

        assertEquals(TopupStatus.FAILED, tx.getTopupStatus());
        assertTrue(tx.isCallbackReceived());
        assertFalse(tx.isWalletCredited());
        assertEquals(0, new BigDecimal("10.00").compareTo(wallet.getBalance()));
        verify(walletRepository, never()).save(any());
        verify(orgNotificationService, never()).notifyTopUpSuccess(any(), any(), any(), any());
    }

    @Test
    void multipleProcessingQueriesStayPendingUntilSuccessfulCallback() throws Exception {
        WalletTransaction tx = pendingTopup();
        stubLookup(tx);
        when(mpesaDarajaClient.queryStkStatus(CHECKOUT)).thenReturn(processingQuery());

        for (int i = 0; i < 3; i++) {
            StkPushResponse pending = walletService.checkTopUpTransaction(organization.getId(), tx.getId());
            assertEquals(TopupStatus.PENDING, pending.getStatus());
            assertFalse(pending.isWalletCredited());
        }

        stubCredit();
        walletService.handleStkCallback(successCallback());

        assertEquals(TopupStatus.COMPLETED, tx.getTopupStatus());
        assertTrue(tx.isWalletCredited());
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    void successfulCallbackBeforeFirstPollCreditsAndLaterPollDoesNotChangeIt() throws Exception {
        WalletTransaction tx = pendingTopup();
        stubLookup(tx);
        stubCredit();

        walletService.handleStkCallback(successCallback());
        assertEquals(TopupStatus.COMPLETED, tx.getTopupStatus());
        assertTrue(tx.isWalletCredited());

        StkPushResponse later = walletService.checkTopUpTransaction(organization.getId(), tx.getId());

        assertEquals(TopupStatus.COMPLETED, later.getStatus());
        assertTrue(later.isCallbackReceived());
        assertTrue(later.isWalletCredited());
        assertEquals(RECEIPT, later.getMpesaReceipt());
        verify(mpesaDarajaClient, never()).queryStkStatus(any());
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    void prematureFailed4999RecoversWhenSafaricomQuerySucceeds() {
        WalletTransaction tx = pendingTopup();
        tx.setTopupStatus(TopupStatus.FAILED);
        tx.setResultCode("4999");
        tx.setResultDesc("The transaction is still under processing");
        stubLookup(tx);
        stubCredit();
        when(mpesaDarajaClient.queryStkStatus(CHECKOUT)).thenReturn(successQuery());

        StkPushResponse response = walletService.checkTopUpTransaction(organization.getId(), tx.getId());

        assertEquals(TopupStatus.COMPLETED, response.getStatus());
        assertTrue(response.isWalletCredited());
        assertEquals(0, new BigDecimal("11.00").compareTo(wallet.getBalance()));
        verify(mpesaDarajaClient).queryStkStatus(CHECKOUT);
        verify(walletRepository).save(wallet);
    }

    @Test
    void prematureFailed4999KeepsFailedWhenSafaricomReportsTimeout() {
        WalletTransaction tx = pendingTopup();
        tx.setTopupStatus(TopupStatus.FAILED);
        tx.setResultCode("4999");
        tx.setResultDesc("The transaction is still under processing");
        stubLookup(tx);
        when(mpesaDarajaClient.queryStkStatus(CHECKOUT)).thenReturn(timeoutQuery());

        StkPushResponse response = walletService.checkTopUpTransaction(organization.getId(), tx.getId());

        assertEquals(TopupStatus.FAILED, response.getStatus());
        assertFalse(response.isWalletCredited());
        assertEquals("1037", tx.getResultCode());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void pollingCompletedTransactionLeavesItCompleted() {
        WalletTransaction tx = pendingTopup();
        tx.setTopupStatus(TopupStatus.COMPLETED);
        tx.setWalletCredited(true);
        tx.setCallbackReceived(true);
        tx.setMpesaReceipt(RECEIPT);
        stubLookup(tx);

        StkPushResponse response = walletService.checkTopUpTransaction(organization.getId(), tx.getId());

        assertEquals(TopupStatus.COMPLETED, response.getStatus());
        assertTrue(response.isWalletCredited());
        verify(mpesaDarajaClient, never()).queryStkStatus(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void lateC2bOnQueryCompletedTopupSavesReceiptWithoutSecondCredit() throws Exception {
        WalletTransaction tx = pendingTopup();
        tx.setTopupStatus(TopupStatus.COMPLETED);
        tx.setWalletCredited(true);
        tx.setMpesaReceipt(null);
        stubLookup(tx);
        stubC2bOrg();
        when(walletTransactionRepository.findC2bAttachCandidates(eq(organization.getId()), any(), any(Instant.class)))
                .thenReturn(List.of(tx));
        when(walletTransactionRepository.findByIdAndOrganizationIdForUpdate(tx.getId(), organization.getId()))
                .thenReturn(Optional.of(tx));
        when(walletTransactionRepository.findByReference(C2B_RECEIPT)).thenReturn(Optional.empty());
        when(walletTransactionRepository.findByMpesaReceipt(C2B_RECEIPT)).thenReturn(Optional.empty());

        walletService.handleC2bConfirmation(c2bConfirmation(C2B_RECEIPT));

        assertEquals(TopupStatus.COMPLETED, tx.getTopupStatus());
        assertTrue(tx.isWalletCredited());
        assertEquals(C2B_RECEIPT, tx.getMpesaReceipt());
        assertEquals(C2B_RECEIPT, tx.getReference());
        assertEquals(0, new BigDecimal("10.00").compareTo(wallet.getBalance()));
        verify(walletRepository, never()).save(any());
        verify(orgNotificationService, never()).notifyTopUpSuccess(any(), any(), any(), any());
    }

    @Test
    void duplicateC2bConfirmationDoesNotCreditTwice() throws Exception {
        WalletTransaction tx = pendingTopup();
        tx.setTopupStatus(TopupStatus.COMPLETED);
        tx.setWalletCredited(true);
        tx.setMpesaReceipt(C2B_RECEIPT);
        tx.setReference(C2B_RECEIPT);
        stubLookup(tx);
        when(walletTransactionRepository.findByReference(C2B_RECEIPT)).thenReturn(Optional.of(tx));

        walletService.handleC2bConfirmation(c2bConfirmation(C2B_RECEIPT));
        walletService.handleC2bConfirmation(c2bConfirmation(C2B_RECEIPT));

        assertEquals(0, new BigDecimal("10.00").compareTo(wallet.getBalance()));
        verify(walletRepository, never()).findByOrganizationIdForUpdate(any());
        verify(orgNotificationService, never()).notifyTopUpSuccess(any(), any(), any(), any());
    }

    @Test
    void c2bOnPendingStkCreditsOnceAndSavesReceipt() throws Exception {
        WalletTransaction tx = pendingTopup();
        stubLookup(tx);
        stubCredit();
        stubC2bOrg();
        when(walletTransactionRepository.findC2bAttachCandidates(eq(organization.getId()), any(), any(Instant.class)))
                .thenReturn(List.of(tx));
        when(walletTransactionRepository.findByIdAndOrganizationIdForUpdate(tx.getId(), organization.getId()))
                .thenReturn(Optional.of(tx));
        when(walletTransactionRepository.findByReference(C2B_RECEIPT)).thenReturn(Optional.empty());
        when(walletTransactionRepository.findByMpesaReceipt(C2B_RECEIPT)).thenReturn(Optional.empty());

        walletService.handleC2bConfirmation(c2bConfirmation(C2B_RECEIPT));

        assertEquals(TopupStatus.COMPLETED, tx.getTopupStatus());
        assertTrue(tx.isWalletCredited());
        assertEquals(C2B_RECEIPT, tx.getMpesaReceipt());
        assertEquals(0, new BigDecimal("11.00").compareTo(wallet.getBalance()));
        verify(walletRepository, times(1)).save(wallet);
        verify(orgNotificationService).notifyTopUpSuccess(any(), any(), any(), eq(C2B_RECEIPT));
        verify(orgNotificationService).notifyPlatformOwnerTopUp(any(), any(), any(), eq(C2B_RECEIPT));
    }

    @Test
    void hashedC2bMsisdnKeepsExistingStkPhoneAndStillCredits() throws Exception {
        WalletTransaction tx = pendingTopup();
        stubLookup(tx);
        stubCredit();
        stubC2bOrg();
        when(walletTransactionRepository.findC2bAttachCandidates(eq(organization.getId()), any(), any(Instant.class)))
                .thenReturn(List.of(tx));
        when(walletTransactionRepository.findByIdAndOrganizationIdForUpdate(tx.getId(), organization.getId()))
                .thenReturn(Optional.of(tx));
        when(walletTransactionRepository.findByReference(C2B_RECEIPT)).thenReturn(Optional.empty());
        when(walletTransactionRepository.findByMpesaReceipt(C2B_RECEIPT)).thenReturn(Optional.empty());

        walletService.handleC2bConfirmation(c2bConfirmation(C2B_RECEIPT, HASHED_MSISDN));

        assertEquals(TopupStatus.COMPLETED, tx.getTopupStatus());
        assertTrue(tx.isWalletCredited());
        assertEquals("254711766223", tx.getPhoneNumber());
        assertEquals(C2B_RECEIPT, tx.getMpesaReceipt());
        assertEquals(0, new BigDecimal("11.00").compareTo(wallet.getBalance()));
    }

    @Test
    void standaloneC2bStoresHashedMsisdnInPhoneNumber() throws Exception {
        stubC2bOrg();
        when(walletTransactionRepository.findByReference(C2B_RECEIPT)).thenReturn(Optional.empty());
        when(walletTransactionRepository.findByMpesaReceipt(C2B_RECEIPT)).thenReturn(Optional.empty());
        when(walletTransactionRepository.findC2bAttachCandidates(eq(organization.getId()), any(), any(Instant.class)))
                .thenReturn(List.of());
        when(walletRepository.findByOrganizationIdForUpdate(organization.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.handleC2bConfirmation(c2bConfirmation(C2B_RECEIPT, HASHED_MSISDN));

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(captor.capture());
        WalletTransaction saved = captor.getValue();
        assertEquals(HASHED_MSISDN, saved.getPhoneNumber());
        assertEquals(64, saved.getPhoneNumber().length());
        assertTrue(saved.isWalletCredited());
        assertEquals(0, new BigDecimal("11.00").compareTo(wallet.getBalance()));
    }

    @Test
    void collectionAccountC2bIsRecordedWithoutWalletCredit() throws Exception {
        when(walletTransactionRepository.findByReference(C2B_RECEIPT)).thenReturn(Optional.empty());
        when(walletTransactionRepository.findByMpesaReceipt(C2B_RECEIPT)).thenReturn(Optional.empty());

        walletService.handleC2bConfirmation(c2bConfirmation(C2B_RECEIPT, HASHED_MSISDN, "sheila"));

        verify(paybillCollectionService).record(
                eq(C2B_RECEIPT),
                any(),
                eq("sheila"),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class));
        verify(walletRepository, never()).findByOrganizationIdForUpdate(any());
        verify(walletTransactionRepository, never()).save(any());
        verify(orgNotificationService, never()).notifyTopUpSuccess(any(), any(), any(), any());
    }

    @Test
    void collectionAccountValidationIsAccepted() throws Exception {
        var result = walletService.handleC2bValidation(c2bConfirmation(C2B_RECEIPT, "254711766223", "Keneth"));

        assertEquals("0", result.get("ResultCode"));
    }

    @Test
    void standaloneC2bCreditsOnceAsPaybill() throws Exception {
        stubC2bOrg();
        when(walletTransactionRepository.findByReference(C2B_RECEIPT)).thenReturn(Optional.empty());
        when(walletTransactionRepository.findByMpesaReceipt(C2B_RECEIPT)).thenReturn(Optional.empty());
        when(walletTransactionRepository.findC2bAttachCandidates(eq(organization.getId()), any(), any(Instant.class)))
                .thenReturn(List.of());
        when(walletRepository.findByOrganizationIdForUpdate(organization.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.handleC2bConfirmation(c2bConfirmation(C2B_RECEIPT));

        assertEquals(0, new BigDecimal("11.00").compareTo(wallet.getBalance()));
        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(captor.capture());
        WalletTransaction saved = captor.getValue();
        assertEquals(PaymentMethod.PAYBILL, saved.getPaymentMethod());
        assertEquals(C2B_RECEIPT, saved.getMpesaReceipt());
        assertEquals(BILL_REF, saved.getBillRef());
        assertTrue(saved.isWalletCredited());
        verify(orgNotificationService).notifyTopUpSuccess(any(), any(), any(), eq(C2B_RECEIPT));
        verify(orgNotificationService).notifyPlatformOwnerTopUp(any(), any(), any(), eq(C2B_RECEIPT));
    }

    @Test
    void lateStkCallbackAfterQueryCreditBackfillsReceiptWithoutSecondCredit() throws Exception {
        WalletTransaction tx = pendingTopup();
        tx.setTopupStatus(TopupStatus.COMPLETED);
        tx.setWalletCredited(true);
        tx.setMpesaReceipt(null);
        stubLookup(tx);

        walletService.handleStkCallback(successCallback());

        assertEquals(TopupStatus.COMPLETED, tx.getTopupStatus());
        assertTrue(tx.isWalletCredited());
        assertEquals(RECEIPT, tx.getMpesaReceipt());
        assertEquals(0, new BigDecimal("10.00").compareTo(wallet.getBalance()));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void duplicateCallbackOnCompletedTransactionDoesNotCreditAgain() throws Exception {
        WalletTransaction tx = pendingTopup();
        tx.setTopupStatus(TopupStatus.COMPLETED);
        tx.setWalletCredited(true);
        tx.setCallbackReceived(true);
        tx.setMpesaReceipt(RECEIPT);
        tx.setReference(RECEIPT);
        stubLookup(tx);

        walletService.handleStkCallback(successCallback());

        assertEquals(TopupStatus.COMPLETED, tx.getTopupStatus());
        assertEquals(0, new BigDecimal("10.00").compareTo(wallet.getBalance()));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void prematureFailedStatusRecoversWhenCallbackSucceeds() throws Exception {
        WalletTransaction tx = pendingTopup();
        tx.setTopupStatus(TopupStatus.FAILED);
        tx.setResultDesc("The transaction is still under processing");
        stubLookup(tx);
        stubCredit();

        walletService.handleStkCallback(successCallback());

        assertEquals(TopupStatus.COMPLETED, tx.getTopupStatus());
        assertTrue(tx.isWalletCredited());
        assertTrue(tx.isCallbackReceived());
        assertEquals(RECEIPT, tx.getMpesaReceipt());
        assertEquals(0, new BigDecimal("11.00").compareTo(wallet.getBalance()));
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    void verifyReceiptFindsOwnWalletCreditIgnoringCase() {
        WalletTransaction tx = completedPaybill("UHJA541HGH");
        when(walletTransactionRepository.findByMpesaReceiptIgnoreCase("UHJA541HGH")).thenReturn(Optional.of(tx));

        var result = walletService.verifyReceipt(organization.getId(), "uhja541hgh");

        assertTrue(result.isFound());
        assertTrue(result.isWalletCredited());
        assertEquals("WALLET", result.getSource());
        assertEquals("UHJA541HGH", result.getMpesaReceipt());
    }

    @Test
    void verifyReceiptHidesOtherOrganization() {
        Organization other = Organization.builder().id(UUID.randomUUID()).name("Other").build();
        WalletTransaction tx = completedPaybill("UHJA541HGH");
        tx.setOrganization(other);
        when(walletTransactionRepository.findByMpesaReceiptIgnoreCase("UHJA541HGH")).thenReturn(Optional.of(tx));

        var result = walletService.verifyReceipt(organization.getId(), "UHJA541HGH");

        assertFalse(result.isFound());
        assertEquals("NONE", result.getSource());
    }

    @Test
    void verifyReceiptPlatformFindsCollectionAccount() {
        when(paybillCollectionService.findByReceipt("UHJA541HGH")).thenReturn(Optional.of(PaybillCollection.builder()
                .id(UUID.randomUUID())
                .billRef("SHEILA")
                .amount(new BigDecimal("100.00"))
                .mpesaReceipt("UHJA541HGH")
                .build()));

        var result = walletService.verifyReceiptPlatform("UHJA541HGH");

        assertTrue(result.isFound());
        assertEquals("COLLECTION", result.getSource());
        assertFalse(result.isWalletCredited());
        assertEquals("SHEILA", result.getBillRef());
    }

    @Test
    void creditByMpesaReceiptCreditsMissedPaybill() {
        stubC2bOrg();
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        stubCredit();
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = walletService.creditByMpesaReceipt(
                "novatest", "uhja541hgh", new BigDecimal("100.00"));

        assertTrue(result.isFound());
        assertTrue(result.isWalletCredited());
        assertEquals("UHJA541HGH", result.getMpesaReceipt());
        assertEquals(organization.getId(), result.getOrganizationId());
        assertEquals(0, new BigDecimal("110.00").compareTo(wallet.getBalance()));
        verify(orgNotificationService).notifyTopUpSuccess(any(), any(), any(), eq("UHJA541HGH"));
        verify(orgNotificationService).notifyPlatformOwnerTopUp(any(), any(), any(), eq("UHJA541HGH"));
    }

    @Test
    void creditByMpesaReceiptIsIdempotentForExistingReceipt() {
        stubC2bOrg();
        WalletTransaction tx = completedPaybill("UHJA541HGH");
        when(walletTransactionRepository.findByMpesaReceiptIgnoreCase("UHJA541HGH")).thenReturn(Optional.of(tx));

        var result = walletService.creditByMpesaReceipt(
                BILL_REF, "UHJA541HGH", new BigDecimal("100.00"));

        assertTrue(result.isWalletCredited());
        verify(walletRepository, never()).save(any());
        verify(orgNotificationService, never()).notifyTopUpSuccess(any(), any(), any(), any());
    }

    @Test
    void creditByMpesaReceiptRejectsUnknownAccount() {
        assertThrows(ApiException.class, () -> walletService.creditByMpesaReceipt(
                "NOVAZZZZ", "UHJA541HGH", new BigDecimal("100.00")));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void creditByMpesaReceiptRejectsCollectionAccount() {
        assertThrows(ApiException.class, () -> walletService.creditByMpesaReceipt(
                "sheila", "UHJA541HGH", new BigDecimal("100.00")));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void verifyReceiptPlatformCreditsFromStoredC2bCallback() {
        stubC2bOrg();
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        stubCredit();
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(c2bInboundService.findByReceipt("UHJA541HGH")).thenReturn(Optional.of(storedInbound()));

        var result = walletService.verifyReceiptPlatform("UHJA541HGH");

        assertTrue(result.isWalletCredited());
        assertEquals(organization.getId(), result.getOrganizationId());
        assertEquals(0, new BigDecimal("110.00").compareTo(wallet.getBalance()));
        verify(c2bInboundService).markCredited("UHJA541HGH");
    }

    @Test
    void verifyReceiptDoesNotCreditInboundForAnotherOrganization() {
        stubC2bOrg();
        when(c2bInboundService.findByReceipt("UHJA541HGH")).thenReturn(Optional.of(storedInbound()));

        var result = walletService.verifyReceipt(UUID.randomUUID(), "UHJA541HGH");

        assertFalse(result.isFound());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void creditByMpesaReceiptUsesStoredBillRefNotFrontendAccount() {
        stubC2bOrg();
        when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));
        stubCredit();
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(c2bInboundService.findByReceipt("UHJA541HGH")).thenReturn(Optional.of(storedInbound()));

        var result = walletService.creditByMpesaReceipt(null, "UHJA541HGH", null);

        assertTrue(result.isWalletCredited());
        assertEquals(organization.getId(), result.getOrganizationId());
        verify(c2bInboundService).markCredited("UHJA541HGH");
    }

    @Test
    void creditByMpesaReceiptRejectsMismatchedAccountWhenInboundExists() {
        when(c2bInboundService.findByReceipt("UHJA541HGH")).thenReturn(Optional.of(storedInbound()));

        assertThrows(ApiException.class, () -> walletService.creditByMpesaReceipt(
                "NOVAOTHR", "UHJA541HGH", new BigDecimal("100.00")));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void verifyReceiptQueriesSafaricomWhenCallbackIsMissing() {
        when(mpesaDarajaClient.isTransactionStatusConfigured()).thenReturn(true);
        when(transactionStatusQueryRepository
                .findFirstByMpesaReceiptIgnoreCaseAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                        eq("UHJA541HGH"), eq(TransactionStatusQueryState.PENDING), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(mpesaDarajaClient.queryTransactionStatus(eq("UHJA541HGH"), any(), any()))
                .thenReturn(new MpesaDarajaClient.TransactionStatusSubmitResult(
                        "orig-1", "conv-1", "0", "Accept the service request successfully.", "{}"));
        when(transactionStatusQueryRepository.save(any(MpesaTransactionStatusQuery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = walletService.verifyReceipt(organization.getId(), "UHJA541HGH");

        assertFalse(result.isFound());
        assertEquals("SAFARICOM_QUERY", result.getSource());
        assertFalse(result.isNeedsManualRecovery());
        verify(mpesaDarajaClient).queryTransactionStatus(eq("UHJA541HGH"),
                eq("https://smsapi.novastack.co.ke/api/v1/payments/transaction-status/result"),
                eq("https://smsapi.novastack.co.ke/api/v1/payments/transaction-status/timeout"));
    }

    @Test
    void transactionStatusResultCreditsFromSafaricomBillRef() throws Exception {
        stubC2bOrg();
        stubCredit();
        when(walletTransactionRepository.findByReference("UHJA541HGH")).thenReturn(Optional.empty());
        when(walletTransactionRepository.findByMpesaReceipt("UHJA541HGH")).thenReturn(Optional.empty());
        when(walletTransactionRepository.findC2bAttachCandidates(eq(organization.getId()), any(), any(Instant.class)))
                .thenReturn(List.of());
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionStatusQueryRepository.findFirstByOriginatorConversationId("orig-1"))
                .thenReturn(Optional.empty());
        when(transactionStatusQueryRepository.save(any(MpesaTransactionStatusQuery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        walletService.handleTransactionStatusResult(transactionStatusResultJson(
                "UHJA541HGH", "100.00", BILL_REF, "Completed"));

        assertEquals(0, new BigDecimal("110.00").compareTo(wallet.getBalance()));
        verify(walletRepository).save(wallet);
        verify(c2bInboundService).markCredited("UHJA541HGH");
    }

    @Test
    void transactionStatusResultDoesNotCreditWithoutBillRef() throws Exception {
        when(transactionStatusQueryRepository.findFirstByOriginatorConversationId("orig-1"))
                .thenReturn(Optional.empty());
        when(transactionStatusQueryRepository.save(any(MpesaTransactionStatusQuery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        walletService.handleTransactionStatusResult(transactionStatusResultJson(
                "UHJA541HGH", "100.00", "", "Completed"));

        verify(walletRepository, never()).save(any());
        verify(walletTransactionRepository, never()).save(any());
    }

    private void stubLookup(WalletTransaction tx) {
        lenient().when(walletTransactionRepository.findByIdAndOrganizationId(tx.getId(), organization.getId()))
                .thenReturn(Optional.of(tx));
        lenient().when(walletTransactionRepository.findByCheckoutRequestIdForUpdate(CHECKOUT))
                .thenReturn(Optional.of(tx));
        lenient().when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubCredit() {
        lenient().when(walletRepository.findByOrganizationIdForUpdate(organization.getId())).thenReturn(Optional.of(wallet));
        lenient().when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(walletTransactionRepository.findByReference(RECEIPT)).thenReturn(Optional.empty());
        lenient().when(walletTransactionRepository.findByReference(C2B_RECEIPT)).thenReturn(Optional.empty());
    }

    private void stubC2bOrg() {
        lenient().when(organizationRepository.findByMpesaAccountRefIgnoreCase(BILL_REF))
                .thenReturn(Optional.of(organization));
    }

    private MpesaC2bInbound storedInbound() {
        return MpesaC2bInbound.builder()
                .id(UUID.randomUUID())
                .mpesaReceipt("UHJA541HGH")
                .billRef(BILL_REF)
                .amount(new BigDecimal("100.00"))
                .credited(false)
                .build();
    }

    private WalletTransaction completedPaybill(String receipt) {
        return WalletTransaction.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .wallet(wallet)
                .type(WalletTransactionType.TOPUP)
                .amount(new BigDecimal("100.00"))
                .mpesaReceipt(receipt)
                .reference(receipt)
                .topupStatus(TopupStatus.COMPLETED)
                .walletCredited(true)
                .paymentMethod(PaymentMethod.PAYBILL)
                .billRef(BILL_REF)
                .build();
    }

    private WalletTransaction pendingTopup() {
        return WalletTransaction.builder()
                .id(UUID.randomUUID())
                .organization(organization)
                .wallet(wallet)
                .type(WalletTransactionType.TOPUP)
                .amount(new BigDecimal("1.00"))
                .balanceBefore(new BigDecimal("10.00"))
                .balanceAfter(new BigDecimal("10.00"))
                .reference("STK-pending")
                .phoneNumber("254711766223")
                .checkoutRequestId(CHECKOUT)
                .merchantRequestId("m-1")
                .topupStatus(TopupStatus.PENDING)
                .callbackReceived(false)
                .walletCredited(false)
                .build();
    }

    private static MpesaDarajaClient.StkQueryResult processingQuery() {
        return new MpesaDarajaClient.StkQueryResult(
                "500.001.1001",
                "The transaction is still under processing",
                "m-1",
                CHECKOUT,
                "500.001.1001",
                "The transaction is still under processing",
                "{\"ResultDesc\":\"The transaction is still under processing\"}");
    }

    private static MpesaDarajaClient.StkQueryResult successQuery() {
        return new MpesaDarajaClient.StkQueryResult(
                "0",
                "The service request has been accepted successfully",
                "m-1",
                CHECKOUT,
                "0",
                "The service request is processed successfully.",
                "{\"ResultCode\":\"0\"}");
    }

    private static MpesaDarajaClient.StkQueryResult timeoutQuery() {
        return new MpesaDarajaClient.StkQueryResult(
                "0",
                "The service request has been accepted successfully",
                "m-1",
                CHECKOUT,
                "1037",
                "DS timeout user cannot be reached.",
                "{\"ResultCode\":\"1037\"}");
    }

    private static JsonNode successCallback() throws Exception {
        String json = """
                {
                  "Body": {
                    "stkCallback": {
                      "MerchantRequestID": "m-1",
                      "CheckoutRequestID": "%s",
                      "ResultCode": 0,
                      "ResultDesc": "The service request is processed successfully.",
                      "CallbackMetadata": {
                        "Item": [
                          {"Name": "Amount", "Value": 1.00},
                          {"Name": "MpesaReceiptNumber", "Value": "%s"},
                          {"Name": "TransactionDate", "Value": 20260819104512},
                          {"Name": "PhoneNumber", "Value": 254711766223}
                        ]
                      }
                    }
                  }
                }
                """.formatted(CHECKOUT, RECEIPT);
        return OBJECT_MAPPER.readTree(json);
    }

    private static JsonNode c2bConfirmation(String receipt) throws Exception {
        return c2bConfirmation(receipt, "254711766223", BILL_REF);
    }

    private static JsonNode c2bConfirmation(String receipt, String msisdn) throws Exception {
        return c2bConfirmation(receipt, msisdn, BILL_REF);
    }

    private static JsonNode c2bConfirmation(String receipt, String msisdn, String billRef) throws Exception {
        String json = """
                {
                  "TransID": "%s",
                  "TransAmount": "1.00",
                  "BillRefNumber": "%s",
                  "MSISDN": "%s",
                  "TransTime": "20260819120100",
                  "BusinessShortCode": "5687394",
                  "FirstName": "Keneth"
                }
                """.formatted(receipt, billRef, msisdn);
        return OBJECT_MAPPER.readTree(json);
    }

    private static JsonNode transactionStatusResultJson(
            String receipt,
            String amount,
            String billRef,
            String transactionStatus) throws Exception {
        String json = """
                {
                  "Result": {
                    "ResultType": 0,
                    "ResultCode": 0,
                    "ResultDesc": "The service request is processed successfully.",
                    "OriginatorConversationID": "orig-1",
                    "ConversationID": "AG_test",
                    "TransactionID": "%s",
                    "ResultParameters": {
                      "ResultParameter": [
                        {"Key": "ReceiptNo", "Value": "%s"},
                        {"Key": "Amount", "Value": "%s"},
                        {"Key": "TransactionStatus", "Value": "%s"},
                        {"Key": "BillReferenceNumber", "Value": "%s"},
                        {"Key": "DebitPartyName", "Value": "254711766223 - Keneth"}
                      ]
                    }
                  }
                }
                """.formatted(receipt, receipt, amount, transactionStatus, billRef);
        return OBJECT_MAPPER.readTree(json);
    }

    private static JsonNode failedCallback(int resultCode, String desc) throws Exception {
        String json = """
                {
                  "Body": {
                    "stkCallback": {
                      "MerchantRequestID": "m-1",
                      "CheckoutRequestID": "%s",
                      "ResultCode": %d,
                      "ResultDesc": "%s"
                    }
                  }
                }
                """.formatted(CHECKOUT, resultCode, desc);
        return OBJECT_MAPPER.readTree(json);
    }
}
