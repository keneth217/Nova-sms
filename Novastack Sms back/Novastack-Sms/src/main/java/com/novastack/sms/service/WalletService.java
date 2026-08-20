package com.novastack.sms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.MpesaC2bInbound;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.entity.PaybillCollection;
import com.novastack.sms.domain.entity.Wallet;
import com.novastack.sms.domain.entity.WalletTransaction;
import com.novastack.sms.domain.enums.PaymentMethod;
import com.novastack.sms.domain.enums.TopupStatus;
import com.novastack.sms.domain.enums.WalletTransactionType;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.domain.repository.WalletRepository;
import com.novastack.sms.domain.repository.WalletTransactionRepository;
import com.novastack.sms.dto.request.WalletTopupRequest;
import com.novastack.sms.dto.response.MpesaReceiptLookupResponse;
import com.novastack.sms.dto.response.StkPushResponse;
import com.novastack.sms.dto.response.WalletBalanceResponse;
import com.novastack.sms.dto.response.WalletTransactionResponse;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.mpesa.C2bCallbackResponses;
import com.novastack.sms.mpesa.MpesaDarajaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    /** Matches wallet_transactions.phone_number (plain 254… or 64-char C2B hash). */
    private static final int PHONE_COLUMN_LENGTH = 64;

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final OrganizationRepository organizationRepository;
    private final AppProperties appProperties;
    private final MpesaDarajaClient mpesaDarajaClient;
    private final BillingSettingsService billingSettingsService;
    private final SmsBillingCalculator smsBillingCalculator;
    private final OrgNotificationService orgNotificationService;
    private final PaybillCollectionService paybillCollectionService;
    private final C2bInboundService c2bInboundService;

    /**
     * Creates a zero-balance prepaid wallet for a new organization (register / ensure).
     * Idempotent: returns the existing wallet if one is already present.
     */
    @Transactional
    public Wallet createForOrganization(Organization organization) {
        return walletRepository.findByOrganizationId(organization.getId())
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .organization(organization)
                        .balance(BigDecimal.ZERO)
                        .currency("KES")
                        .build()));
    }

    /** Find-or-create wallet so top-up and SMS never fail with "Wallet not found". */
    @Transactional
    public Wallet ensureWallet(UUID organizationId) {
        return walletRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> createForOrganization(getOrganization(organizationId)));
    }

    /** Startup backfill for orgs that were created before wallets were guaranteed. */
    @Transactional
    public int backfillMissingWallets() {
        int created = 0;
        for (Organization org : organizationRepository.findAll()) {
            if (walletRepository.findByOrganizationId(org.getId()).isEmpty()) {
                createForOrganization(org);
                created++;
                log.info("Created missing wallet for organization {}", org.getId());
            }
        }
        return created;
    }

    @Transactional
    public WalletBalanceResponse getBalance(UUID organizationId) {
        Organization org = ensureMpesaAccountRef(getOrganization(organizationId));
        Wallet wallet = ensureWallet(organizationId);
        BigDecimal smsCost = billingSettingsService.customerPrice();
        return WalletBalanceResponse.builder()
                .walletId(wallet.getId())
                .organizationId(organizationId)
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency() != null ? wallet.getCurrency() : billingSettingsService.currency())
                .smsCost(smsCost)
                .availableSms(smsBillingCalculator.availableSms(wallet.getBalance()))
                .paybill(appProperties.getMpesa().getShortcode())
                .accountNumber(org.getMpesaAccountRef())
                .businessName("Novastack SMS")
                .build();
    }

    /**
     * Initiates M-Pesa Daraja STK Push to the configured Paybill.
     * Wallet is credited only after a successful callback.
     */
    @Transactional
    public StkPushResponse initiateTopUp(UUID organizationId, WalletTopupRequest request) {
        Organization org = ensureMpesaAccountRef(getOrganization(organizationId));
        Wallet wallet = ensureWallet(organizationId);
        String phone = normalizePhone(request.getPhoneNumber());
        String accountReference = org.getMpesaAccountRef();
        String callbackUrl = trimSlash(appProperties.getMpesa().getCallbackBaseUrl())
                + "/api/v1/mpesa/stk/callback";

        String pendingReference = "STK-" + UUID.randomUUID().toString().replace("-", "");

        WalletTransaction pending = walletTransactionRepository.saveAndFlush(WalletTransaction.builder()
                .organization(org)
                .wallet(wallet)
                .type(WalletTransactionType.TOPUP)
                .amount(request.getAmount())
                .balanceBefore(wallet.getBalance())
                .balanceAfter(wallet.getBalance())
                .reference(pendingReference)
                .phoneNumber(phone)
                .topupStatus(TopupStatus.PENDING)
                .paymentMethod(PaymentMethod.STK_PUSH)
                .billRef(accountReference)
                .description("M-Pesa STK Push to Paybill " + appProperties.getMpesa().getShortcode()
                        + " (account " + accountReference + ")")
                .build());

        MpesaDarajaClient.StkPushResult stk;
        try {
            stk = mpesaDarajaClient.initiateStkPush(
                    phone, request.getAmount(), accountReference, callbackUrl);
        } catch (RuntimeException ex) {
            pending.setTopupStatus(TopupStatus.FAILED);
            pending.setResultDesc(ex.getMessage());
            walletTransactionRepository.save(pending);
            throw ex;
        }

        pending.setCheckoutRequestId(stk.checkoutRequestId());
        pending.setMerchantRequestId(stk.merchantRequestId());
        walletTransactionRepository.save(pending);

        log.info("STK initiated: checkoutRequestId={} org={} amount={}",
                stk.checkoutRequestId(), organizationId, request.getAmount());

        return StkPushResponse.builder()
                .transactionId(pending.getId())
                .checkoutRequestId(stk.checkoutRequestId())
                .merchantRequestId(stk.merchantRequestId())
                .customerMessage(stk.customerMessage())
                .status(TopupStatus.PENDING)
                .amount(request.getAmount())
                .phoneNumber(phone)
                .build();
    }

    @Transactional
    public void handleStkCallback(JsonNode payload) {
        JsonNode body = payload.path("Body").path("stkCallback");
        if (body.isMissingNode()) {
            log.warn("Invalid STK callback payload: {}", payload);
            return;
        }

        String checkoutRequestId = textOrNull(body.path("CheckoutRequestID"));
        String resultCode = body.path("ResultCode").asText();
        String resultDesc = body.path("ResultDesc").asText();

        if (checkoutRequestId == null) {
            log.warn("STK callback missing CheckoutRequestID");
            return;
        }

        log.info("STK callback received: checkoutRequestId={} resultCode={}", checkoutRequestId, resultCode);

        WalletTransaction tx = walletTransactionRepository.findByCheckoutRequestIdForUpdate(checkoutRequestId)
                .orElse(null);
        if (tx == null) {
            log.warn("No pending top-up for CheckoutRequestID={}", checkoutRequestId);
            return;
        }

        if (isSuccessfullyCompleted(tx)) {
            if ("0".equals(resultCode)) {
                applySuccessfulCallbackMetadata(tx, body, resultCode, resultDesc, true);
            }
            log.info("Duplicate callback ignored: checkoutRequestId={} reason=already_completed_and_credited",
                    checkoutRequestId);
            return;
        }

        if (!"0".equals(resultCode)) {
            tx.setCallbackReceived(true);
            markTopUpFailed(tx, resultCode, resultDesc);
            return;
        }

        String mpesaReceipt = extractCallbackMetadata(body, "MpesaReceiptNumber");
        String amountStr = extractCallbackMetadata(body, "Amount");
        String phone = extractCallbackMetadata(body, "PhoneNumber");
        String transactionDate = extractCallbackMetadata(body, "TransactionDate");
        BigDecimal paidAmount = amountStr != null ? new BigDecimal(amountStr) : tx.getAmount();

        completeSuccessfulTopUp(tx, paidAmount, mpesaReceipt, phone, transactionDate, resultCode, resultDesc, true);
    }

    /**
     * Poll our DB for callback status. If still PENDING, query Safaricom STK status and update DB.
     * "Still under processing" is PENDING, never FAILED. COMPLETED is never overwritten.
     */
    @Transactional
    public StkPushResponse checkTopUpTransaction(UUID organizationId, UUID transactionId) {
        WalletTransaction tx = walletTransactionRepository.findByIdAndOrganizationId(transactionId, organizationId)
                .orElseThrow(() -> new ApiException("Top-up transaction not found", HttpStatus.NOT_FOUND));

        if (isSuccessfullyCompleted(tx)) {
            return toStkResponse(tx, "Transaction already finalized in database");
        }

        if (tx.getCheckoutRequestId() == null || tx.getCheckoutRequestId().isBlank()) {
            return toStkResponse(tx, "Waiting for STK initiation to complete");
        }

        log.info("Polling Safaricom STK status for checkoutRequestId={}", tx.getCheckoutRequestId());
        MpesaDarajaClient.StkQueryResult query = mpesaDarajaClient.queryStkStatus(tx.getCheckoutRequestId());

        tx = walletTransactionRepository.findByCheckoutRequestIdForUpdate(tx.getCheckoutRequestId())
                .orElse(tx);

        if (isSuccessfullyCompleted(tx)) {
            return toStkResponse(tx, "Callback already received and database updated");
        }

        if (query.isPaymentSuccessful()) {
            completeSuccessfulTopUp(
                    tx,
                    tx.getAmount(),
                    tx.getMpesaReceipt(),
                    tx.getPhoneNumber(),
                    tx.getMpesaTransactionDate(),
                    query.resultCode(),
                    query.resultDesc() != null ? query.resultDesc() : "Confirmed via STK query",
                    false
            );
            return toStkResponse(tx, "Payment confirmed via Safaricom query; wallet updated");
        }

        if (query.isStillProcessing()) {
            log.info("STK query pending: checkoutRequestId={}", tx.getCheckoutRequestId());
            tx.setResultCode(query.resultCode());
            tx.setResultDesc(query.resultDesc() != null
                    ? query.resultDesc()
                    : query.responseDescription() != null
                            ? query.responseDescription()
                            : "Awaiting Safaricom callback");
            if (tx.getTopupStatus() == TopupStatus.FAILED) {
                tx.setTopupStatus(TopupStatus.PENDING);
            }
            walletTransactionRepository.save(tx);
            return toStkResponse(tx, "Still pending — waiting for Safaricom callback or user PIN entry");
        }

        if (query.isTerminalFailure()) {
            markTopUpFailed(tx, query.resultCode(), query.resultDesc());
            return toStkResponse(tx, "Payment failed/cancelled; database updated");
        }

        return toStkResponse(tx, "Still pending — waiting for Safaricom callback or user PIN entry");
    }

    @Transactional
    public StkPushResponse checkTopUpTransaction(UUID transactionId) {
        WalletTransaction tx = walletTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ApiException("Top-up transaction not found", HttpStatus.NOT_FOUND));
        if (tx.getOrganization() == null) {
            throw new ApiException("Top-up has no organization", HttpStatus.BAD_REQUEST);
        }
        return checkTopUpTransaction(tx.getOrganization().getId(), transactionId);
    }

    @Transactional(readOnly = true)
    public StkPushResponse getTopUpStatus(UUID organizationId, UUID transactionId) {
        WalletTransaction tx = walletTransactionRepository.findByIdAndOrganizationId(transactionId, organizationId)
                .orElseThrow(() -> new ApiException("Top-up transaction not found", HttpStatus.NOT_FOUND));
        return toStkResponse(tx, null);
    }

    private void completeSuccessfulTopUp(
            WalletTransaction tx,
            BigDecimal paidAmount,
            String mpesaReceipt,
            String phone,
            String transactionDate,
            String resultCode,
            String resultDesc,
            boolean fromCallback) {

        if (isSuccessfullyCompleted(tx)) {
            applySuccessfulCallbackMetadata(tx, mpesaReceipt, phone, transactionDate, resultCode, resultDesc, fromCallback);
            return;
        }

        if (mpesaReceipt != null && walletTransactionRepository.findByReference(mpesaReceipt)
                .filter(existing -> !existing.getId().equals(tx.getId()))
                .isPresent()) {
            if (fromCallback) {
                tx.setCallbackReceived(true);
            }
            markTopUpFailed(tx, resultCode, "Duplicate M-Pesa receipt: " + mpesaReceipt);
            return;
        }

        Wallet wallet = walletRepository.findByOrganizationIdForUpdate(tx.getOrganization().getId())
                .orElseThrow(() -> new ApiException("Wallet not found", HttpStatus.NOT_FOUND));

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(paidAmount);
        wallet.setBalance(after);
        walletRepository.save(wallet);

        tx.setAmount(paidAmount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        applyPaymentDetails(tx, mpesaReceipt, phone, transactionDate);
        tx.setResultCode(resultCode);
        tx.setResultDesc(resultDesc);
        tx.setTopupStatus(TopupStatus.COMPLETED);
        tx.setWalletCredited(true);
        if (fromCallback) {
            tx.setCallbackReceived(true);
        }
        tx.setDescription("M-Pesa Paybill " + appProperties.getMpesa().getShortcode()
                + " top-up" + (mpesaReceipt != null ? " receipt " + mpesaReceipt : ""));
        logWalletTransactionSave(tx, phone);
        walletTransactionRepository.save(tx);

        Organization organization = tx.getOrganization();
        Hibernate.initialize(organization);
        runAfterCommit(() -> {
            log.info("Wallet credited: checkoutRequestId={} amount={} receipt={}",
                    tx.getCheckoutRequestId(), paidAmount, mpesaReceipt);
            notifyMpesaWalletTopUp(organization, paidAmount, after, mpesaReceipt);
        });
    }

    private void notifyMpesaWalletTopUp(
            Organization organization, BigDecimal amount, BigDecimal after, String receipt) {
        orgNotificationService.notifyTopUpSuccess(organization, amount, after, receipt);
        orgNotificationService.notifyPlatformOwnerTopUp(organization, amount, after, receipt);
    }

    private void applySuccessfulCallbackMetadata(
            WalletTransaction tx,
            JsonNode stkCallback,
            String resultCode,
            String resultDesc,
            boolean fromCallback) {
        applySuccessfulCallbackMetadata(
                tx,
                extractCallbackMetadata(stkCallback, "MpesaReceiptNumber"),
                extractCallbackMetadata(stkCallback, "PhoneNumber"),
                extractCallbackMetadata(stkCallback, "TransactionDate"),
                resultCode,
                resultDesc,
                fromCallback);
    }

    private void applySuccessfulCallbackMetadata(
            WalletTransaction tx,
            String mpesaReceipt,
            String phone,
            String transactionDate,
            String resultCode,
            String resultDesc,
            boolean fromCallback) {
        applyPaymentDetails(tx, mpesaReceipt, phone, transactionDate);
        if (fromCallback) {
            tx.setCallbackReceived(true);
        }
        if (resultCode != null && !resultCode.isBlank()) {
            tx.setResultCode(resultCode);
        }
        if (resultDesc != null && !resultDesc.isBlank()) {
            tx.setResultDesc(resultDesc);
        }
        walletTransactionRepository.save(tx);
    }

    private void applyPaymentDetails(WalletTransaction tx, String mpesaReceipt, String phone, String transactionDate) {
        if (mpesaReceipt != null && !mpesaReceipt.isBlank()) {
            String existing = tx.getMpesaReceipt();
            if (existing == null || existing.isBlank()) {
                tx.setMpesaReceipt(mpesaReceipt);
                String reference = tx.getReference();
                if (reference == null || reference.isBlank() || reference.startsWith("STK-")) {
                    tx.setReference(mpesaReceipt);
                }
            } else if (!existing.equalsIgnoreCase(mpesaReceipt)) {
                log.warn("Keeping existing receipt {} on tx={}; ignoring {}", existing, tx.getId(), mpesaReceipt);
            }
        }
        if (transactionDate != null && !transactionDate.isBlank()) {
            tx.setMpesaTransactionDate(transactionDate);
        }
        if (phone != null && !phone.isBlank()) {
            String candidate = safePhone(phone);
            if (candidate != null) {
                String existing = tx.getPhoneNumber();
                if (!(isPlainMsisdn(existing) && !isPlainMsisdn(candidate))) {
                    tx.setPhoneNumber(candidate);
                }
            }
        }
    }

    private void markTopUpFailed(WalletTransaction tx, String resultCode, String resultDesc) {
        if (isSuccessfullyCompleted(tx)) {
            log.warn("Refusing to overwrite COMPLETED top-up with FAILED checkoutRequestId={}",
                    tx.getCheckoutRequestId());
            return;
        }
        tx.setResultCode(resultCode);
        tx.setResultDesc(resultDesc);
        tx.setTopupStatus(TopupStatus.FAILED);
        walletTransactionRepository.save(tx);
        log.info("STK payment failed checkoutRequestId={} desc={}", tx.getCheckoutRequestId(), resultDesc);
    }

    private boolean isSuccessfullyCompleted(WalletTransaction tx) {
        return tx.isWalletCredited() || tx.getTopupStatus() == TopupStatus.COMPLETED;
    }

    private StkPushResponse toStkResponse(WalletTransaction tx, String message) {
        return StkPushResponse.builder()
                .transactionId(tx.getId())
                .checkoutRequestId(tx.getCheckoutRequestId())
                .merchantRequestId(tx.getMerchantRequestId())
                .customerMessage(message != null ? message : tx.getResultDesc())
                .status(tx.getTopupStatus())
                .amount(tx.getAmount())
                .phoneNumber(tx.getPhoneNumber())
                .mpesaReceipt(tx.getMpesaReceipt())
                .resultCode(tx.getResultCode())
                .resultDesc(tx.getResultDesc())
                .callbackReceived(tx.isCallbackReceived())
                .walletCredited(tx.isWalletCredited())
                .updatedAt(tx.getCreatedAt())
                .build();
    }

    /**
     * C2B Paybill confirmation. TransID is the M-Pesa receipt.
     * Always ACK ResultCode 0: the customer has already been charged.
     * If STK Query already credited the wallet, TransID is still saved when receipt is blank.
     */
    @Transactional
    public Map<String, Object> handleC2bConfirmation(JsonNode payload) {
        try {
            JsonNode body = unwrapCallback(payload);
            if (body == null) {
                log.warn("C2B confirmation missing payload");
                return C2bCallbackResponses.accepted();
            }

            String receipt = jsonText(body, "TransID", "transactionId");
            String amountStr = jsonText(body, "TransAmount", "amount");
            String billRef = jsonText(body, "BillRefNumber", "accountReference");
            String phone = jsonText(body, "MSISDN", "phoneNumber");
            log.info("C2B confirmation TransID={} Amount={} BillRef={} ShortCode={} MSISDN length={} MSISDN={}",
                    receipt,
                    amountStr,
                    billRef,
                    jsonText(body, "BusinessShortCode", "shortCode"),
                    phone == null ? 0 : phone.length(),
                    phone);

            if (receipt == null || amountStr == null) {
                log.warn("C2B confirmation missing TransID or TransAmount");
                return C2bCallbackResponses.accepted();
            }

            BigDecimal amount;
            try {
                amount = parseKes(amountStr);
            } catch (RuntimeException ex) {
                log.warn("C2B confirmation invalid TransAmount={}", amountStr);
                return C2bCallbackResponses.accepted();
            }

            c2bInboundService.capture(
                    receipt,
                    billRef,
                    amount,
                    phone,
                    jsonText(body, "TransTime"),
                    body.toString());

            if (walletTransactionRepository.findByReference(receipt).isPresent()
                    || walletTransactionRepository.findByMpesaReceipt(receipt).isPresent()
                    || paybillCollectionService.existsByReceipt(receipt)) {
                c2bInboundService.markCredited(receipt);
                return C2bCallbackResponses.accepted();
            }

            if (paybillCollectionService.isCollectionAccount(billRef)) {
                paybillCollectionService.record(
                        receipt,
                        amount,
                        billRef,
                        safePhone(phone),
                        jsonText(body, "TransTime"),
                        jsonText(body, "FirstName"),
                        jsonText(body, "MiddleName"),
                        jsonText(body, "LastName"));
                c2bInboundService.markCredited(receipt);
                return C2bCallbackResponses.accepted();
            }

            UUID organizationId = resolveOrganizationFromBillRef(billRef);
            if (organizationId == null) {
                log.warn("C2B confirmation unknown BillRefNumber={} receipt={}", billRef, receipt);
                return C2bCallbackResponses.accepted();
            }

            Instant since = Instant.now().minus(1, ChronoUnit.DAYS);
            WalletTransaction matchingStk = walletTransactionRepository
                    .findC2bAttachCandidates(organizationId, amount, since)
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (matchingStk != null) {
                WalletTransaction locked = walletTransactionRepository
                        .findByIdAndOrganizationIdForUpdate(matchingStk.getId(), organizationId)
                        .orElse(matchingStk);
                completeSuccessfulTopUp(
                        locked,
                        amount,
                        receipt,
                        phone,
                        jsonText(body, "TransTime"),
                        "0",
                        "Confirmed via C2B TransID " + receipt,
                        true);
                c2bInboundService.markCredited(receipt);
                return C2bCallbackResponses.accepted();
            }

            Wallet wallet = walletRepository.findByOrganizationIdForUpdate(organizationId)
                    .orElseThrow(() -> new ApiException("Wallet not found", HttpStatus.NOT_FOUND));

            BigDecimal before = wallet.getBalance();
            BigDecimal after = before.add(amount);
            wallet.setBalance(after);
            walletRepository.save(wallet);

            WalletTransaction saved = WalletTransaction.builder()
                    .organization(wallet.getOrganization())
                    .wallet(wallet)
                    .type(WalletTransactionType.TOPUP)
                    .amount(amount)
                    .balanceBefore(before)
                    .balanceAfter(after)
                    .reference(receipt)
                    .mpesaReceipt(receipt)
                    .phoneNumber(safePhone(phone))
                    .mpesaTransactionDate(jsonText(body, "TransTime"))
                    .topupStatus(TopupStatus.COMPLETED)
                    .walletCredited(true)
                    .callbackReceived(true)
                    .paymentMethod(PaymentMethod.PAYBILL)
                    .billRef(billRef)
                    .description("M-Pesa Paybill " + appProperties.getMpesa().getShortcode()
                            + " top-up via account " + billRef + " receipt " + receipt)
                    .build();
            logWalletTransactionSave(saved, phone);
            walletTransactionRepository.save(saved);
            c2bInboundService.markCredited(receipt);

            Organization organization = wallet.getOrganization();
            Hibernate.initialize(organization);
            runAfterCommit(() -> {
                log.info("Wallet credited: checkoutRequestId={} amount={} receipt={}",
                        saved.getCheckoutRequestId(), amount, receipt);
                notifyMpesaWalletTopUp(organization, amount, after, receipt);
            });
            return C2bCallbackResponses.accepted();
        } catch (Exception ex) {
            log.error("C2B confirmation processing failed; acknowledging Daraja", ex);
            return C2bCallbackResponses.accepted();
        }
    }

    /**
     * External validation is optional. Unknown account → C2B00012.
     * Any unexpected error → Accepted so M-PESA uses ResponseType Completed instead of spike-arrest.
     */
    public Map<String, Object> handleC2bValidation(JsonNode payload) {
        try {
            JsonNode body = unwrapCallback(payload);
            if (body == null) {
                return C2bCallbackResponses.accepted();
            }
            String amountStr = jsonText(body, "TransAmount", "amount");
            if (amountStr != null) {
                try {
                    parseKes(amountStr);
                } catch (RuntimeException ex) {
                    return C2bCallbackResponses.invalidAmount();
                }
            }
            String billRef = jsonText(body, "BillRefNumber", "accountReference");
            if (billRef == null || billRef.isBlank()) {
                return C2bCallbackResponses.accepted();
            }
            if (paybillCollectionService.isCollectionAccount(billRef)
                    || resolveOrganizationFromBillRef(billRef) != null) {
                return C2bCallbackResponses.accepted();
            }
            return C2bCallbackResponses.invalidAccount();
        } catch (Exception ex) {
            log.error("C2B validation failed; accepting so the payment is not cancelled", ex);
            return C2bCallbackResponses.accepted();
        }
    }

    /**
     * Receipt lookup. If Nova still has the C2B callback (BillRef + amount) and the wallet
     * was never credited, the organization is resolved from BillRefNumber and credited here.
     * organizationId from the caller is only a visibility scope — never the credit target.
     */
    @Transactional
    public MpesaReceiptLookupResponse verifyReceipt(UUID organizationId, String rawReceipt) {
        return verifyAndRecover(rawReceipt, organizationId, false);
    }

    @Transactional
    public MpesaReceiptLookupResponse verifyReceiptPlatform(String rawReceipt) {
        return verifyAndRecover(rawReceipt, null, true);
    }

    /**
     * Manual recovery when the C2B callback was never stored. Organization is resolved from
     * the Paybill account (BillRefNumber). organizationId is never accepted from the client.
     * If inbound C2B metadata exists, its BillRefNumber and amount win over the form.
     */
    @Transactional
    public MpesaReceiptLookupResponse creditByMpesaReceipt(String accountNumber, String rawReceipt, BigDecimal amount) {
        String receipt = normalizeReceipt(rawReceipt);
        MpesaReceiptLookupResponse existing = lookupCreditedReceipt(receipt, null, true);
        if (existing.isFound() && "COLLECTION".equals(existing.getSource())) {
            throw new ApiException(
                    "Receipt " + receipt + " is a collection payment, not a wallet top-up",
                    HttpStatus.CONFLICT);
        }
        if (existing.isFound() && "WALLET".equals(existing.getSource())) {
            return existing;
        }

        Optional<MpesaC2bInbound> inbound = c2bInboundService.findByReceipt(receipt);
        String billRef;
        BigDecimal creditAmount;
        if (inbound.isPresent() && inbound.get().getBillRef() != null && inbound.get().getAmount() != null) {
            MpesaC2bInbound stored = inbound.get();
            billRef = stored.getBillRef();
            creditAmount = stored.getAmount();
            if (accountNumber != null && !accountNumber.isBlank()
                    && !billRef.trim().equalsIgnoreCase(accountNumber.trim())) {
                throw new ApiException(
                        "Receipt " + receipt + " belongs to account " + billRef
                                + ". It cannot credit a different Paybill account.",
                        HttpStatus.CONFLICT);
            }
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0
                    && amount.compareTo(creditAmount) != 0) {
                throw new ApiException(
                        "Receipt " + receipt + " amount is KES "
                                + creditAmount.stripTrailingZeros().toPlainString()
                                + ". It cannot be credited as a different amount.",
                        HttpStatus.CONFLICT);
            }
        } else {
            if (accountNumber == null || accountNumber.isBlank()) {
                throw new ApiException(
                        "Paybill account number from the M-Pesa SMS is required. Nova has no C2B record for this receipt.",
                        HttpStatus.BAD_REQUEST);
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException("Amount must be greater than zero", HttpStatus.BAD_REQUEST);
            }
            billRef = accountNumber.trim();
            creditAmount = amount.setScale(4, RoundingMode.HALF_UP);
        }

        return creditResolvedPaybill(billRef, receipt, creditAmount, inbound.orElse(null));
    }

    private MpesaReceiptLookupResponse verifyAndRecover(String rawReceipt, UUID organizationId, boolean platform) {
        String receipt = normalizeReceipt(rawReceipt);
        MpesaReceiptLookupResponse existing = lookupCreditedReceipt(receipt, organizationId, platform);
        if (existing.isFound()) {
            return existing;
        }

        Optional<MpesaC2bInbound> inbound = c2bInboundService.findByReceipt(receipt);
        if (inbound.isPresent() && !inbound.get().isCredited()
                && inbound.get().getBillRef() != null && inbound.get().getAmount() != null) {
            MpesaC2bInbound stored = inbound.get();
            if (paybillCollectionService.isCollectionAccount(stored.getBillRef())) {
                return lookupCreditedReceipt(receipt, organizationId, platform);
            }
            UUID resolvedOrg = resolveOrganizationFromBillRef(stored.getBillRef());
            if (!platform && (resolvedOrg == null || !resolvedOrg.equals(organizationId))) {
                return notFoundReceipt(receipt, false);
            }
            if (resolvedOrg == null) {
                return recoverableButUnknownAccount(receipt, stored);
            }
            return creditResolvedPaybill(stored.getBillRef(), receipt, stored.getAmount(), stored);
        }

        return notFoundReceipt(receipt, platform);
    }

    private MpesaReceiptLookupResponse lookupCreditedReceipt(String receipt, UUID organizationId, boolean platform) {
        Optional<WalletTransaction> walletHit = findTopUpByReceipt(receipt);
        if (walletHit.isPresent()) {
            WalletTransaction tx = walletHit.get();
            Organization org = tx.getOrganization();
            UUID txOrgId = org == null ? null : org.getId();
            if (!platform && (txOrgId == null || !txOrgId.equals(organizationId))) {
                return notFoundReceipt(receipt, false);
            }
            return toReceiptLookup(tx, org, receipt, true);
        }

        Optional<PaybillCollection> collection = paybillCollectionService.findByReceipt(receipt);
        if (collection.isPresent()) {
            if (!platform) {
                return notFoundReceipt(receipt, false);
            }
            PaybillCollection row = collection.get();
            return MpesaReceiptLookupResponse.builder()
                    .mpesaReceipt(receipt)
                    .found(true)
                    .source("COLLECTION")
                    .walletCredited(false)
                    .needsManualRecovery(false)
                    .recoverableFromCallback(false)
                    .transactionId(row.getId())
                    .amount(row.getAmount())
                    .billRef(row.getBillRef())
                    .message("Receipt " + receipt + " is a collection payment for "
                            + row.getBillRef() + ". It never credits an SMS wallet.")
                    .build();
        }
        return MpesaReceiptLookupResponse.builder()
                .mpesaReceipt(receipt)
                .found(false)
                .source("NONE")
                .walletCredited(false)
                .needsManualRecovery(true)
                .build();
    }

    private MpesaReceiptLookupResponse creditResolvedPaybill(
            String accountNumber,
            String receipt,
            BigDecimal amount,
            MpesaC2bInbound inbound) {
        if (paybillCollectionService.isCollectionAccount(accountNumber)) {
            throw new ApiException(
                    "Account " + accountNumber.trim() + " is a collection account, not an SMS wallet",
                    HttpStatus.CONFLICT);
        }
        UUID organizationId = resolveOrganizationFromBillRef(accountNumber);
        if (organizationId == null) {
            throw new ApiException(
                    "Unknown Paybill account " + accountNumber.trim()
                            + ". Wallets are identified by the M-Pesa account number, not the receipt.",
                    HttpStatus.NOT_FOUND);
        }

        Optional<WalletTransaction> already = findTopUpByReceipt(receipt);
        if (already.isPresent()) {
            WalletTransaction tx = already.get();
            UUID txOrgId = tx.getOrganization() == null ? null : tx.getOrganization().getId();
            if (txOrgId != null && !txOrgId.equals(organizationId)) {
                throw new ApiException(
                        "Receipt " + receipt + " is already credited to another organization",
                        HttpStatus.CONFLICT);
            }
            c2bInboundService.markCredited(receipt);
            return toReceiptLookup(tx, tx.getOrganization(), receipt, true);
        }

        Organization organization = getOrganization(organizationId);
        Wallet wallet = walletRepository.findByOrganizationIdForUpdate(organizationId)
                .orElseThrow(() -> new ApiException("Wallet not found", HttpStatus.NOT_FOUND));

        BigDecimal creditAmount = amount.setScale(4, RoundingMode.HALF_UP);
        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(creditAmount);
        wallet.setBalance(after);
        walletRepository.save(wallet);

        String billRef = organization.getMpesaAccountRef();
        WalletTransaction saved = WalletTransaction.builder()
                .organization(organization)
                .wallet(wallet)
                .type(WalletTransactionType.TOPUP)
                .amount(creditAmount)
                .balanceBefore(before)
                .balanceAfter(after)
                .reference(receipt)
                .mpesaReceipt(receipt)
                .phoneNumber(inbound == null ? null : inbound.getPhoneNumber())
                .mpesaTransactionDate(inbound == null ? null : inbound.getMpesaTransactionDate())
                .topupStatus(TopupStatus.COMPLETED)
                .walletCredited(true)
                .callbackReceived(inbound != null)
                .paymentMethod(PaymentMethod.PAYBILL)
                .billRef(billRef)
                .description("Recovered M-Pesa Paybill " + appProperties.getMpesa().getShortcode()
                        + " top-up via account " + billRef + " receipt " + receipt)
                .build();
        logWalletTransactionSave(saved, inbound == null ? null : inbound.getPhoneNumber());
        walletTransactionRepository.save(saved);
        c2bInboundService.markCredited(receipt);

        Hibernate.initialize(organization);
        runAfterCommit(() -> {
            log.info("Wallet credited from receipt recovery: account={} amount={} receipt={}",
                    billRef, creditAmount, receipt);
            notifyMpesaWalletTopUp(organization, creditAmount, after, receipt);
        });
        return toReceiptLookup(saved, organization, receipt, true);
    }

    private Optional<WalletTransaction> findTopUpByReceipt(String receipt) {
        return walletTransactionRepository.findByMpesaReceiptIgnoreCase(receipt)
                .or(() -> walletTransactionRepository.findByReferenceIgnoreCase(receipt));
    }

    private MpesaReceiptLookupResponse toReceiptLookup(
            WalletTransaction tx,
            Organization org,
            String receipt,
            boolean found) {
        boolean credited = tx.isWalletCredited() || tx.getTopupStatus() == TopupStatus.COMPLETED;
        String message;
        if (credited) {
            message = "Receipt " + receipt + " already credited "
                    + (tx.getAmount() == null ? "" : "KES " + tx.getAmount().stripTrailingZeros().toPlainString() + " ")
                    + "to this SMS wallet.";
        } else {
            message = "Receipt " + receipt + " is on file but the wallet was not credited yet.";
        }
        if (org != null && !credited) {
            message = "Receipt " + receipt + " is on file for " + org.getName()
                    + " but the wallet was not credited yet.";
        } else if (org != null && credited) {
            message = "Receipt " + receipt + " already credited "
                    + (tx.getAmount() == null ? "" : "KES " + tx.getAmount().stripTrailingZeros().toPlainString() + " ")
                    + "to " + org.getName() + ".";
        }
        return MpesaReceiptLookupResponse.builder()
                .mpesaReceipt(receipt)
                .found(found)
                .source("WALLET")
                .walletCredited(credited)
                .needsManualRecovery(false)
                .recoverableFromCallback(false)
                .transactionId(tx.getId())
                .organizationId(org == null ? null : org.getId())
                .organizationName(org == null ? null : org.getName())
                .amount(tx.getAmount())
                .status(tx.getTopupStatus())
                .billRef(tx.getBillRef() != null ? tx.getBillRef() : (org == null ? null : org.getMpesaAccountRef()))
                .message(message)
                .build();
    }

    private MpesaReceiptLookupResponse recoverableButUnknownAccount(String receipt, MpesaC2bInbound inbound) {
        return MpesaReceiptLookupResponse.builder()
                .mpesaReceipt(receipt)
                .found(true)
                .source("C2B_INBOUND")
                .walletCredited(false)
                .needsManualRecovery(true)
                .recoverableFromCallback(true)
                .amount(inbound.getAmount())
                .billRef(inbound.getBillRef())
                .message("Receipt " + receipt + " was received from Safaricom for account "
                        + inbound.getBillRef() + " but that account is not a Nova SMS organization.")
                .build();
    }

    private MpesaReceiptLookupResponse notFoundReceipt(String receipt, boolean platform) {
        String message = platform
                ? "Receipt " + receipt + " is not in Nova. If the M-Pesa SMS is genuine, enter the Paybill account number and amount from that SMS. Nova will resolve the organization from the account, not from a selected org."
                : "Receipt " + receipt + " is not in Nova yet. If you just paid, wait a moment and try again. If you paid earlier, send the receipt and Paybill account from the Safaricom SMS to support.";
        return MpesaReceiptLookupResponse.builder()
                .mpesaReceipt(receipt)
                .found(false)
                .source("NONE")
                .walletCredited(false)
                .needsManualRecovery(true)
                .recoverableFromCallback(false)
                .message(message)
                .build();
    }

    static String normalizeReceipt(String rawReceipt) {
        if (rawReceipt == null || rawReceipt.isBlank()) {
            throw new ApiException("M-Pesa receipt is required", HttpStatus.BAD_REQUEST);
        }
        return rawReceipt.trim().toUpperCase(Locale.ROOT);
    }

    /** Super-admin allocation / internal funding. Still a real wallet credit — SMS continues to debit. */
    @Transactional
    public void adjust(UUID organizationId, BigDecimal amount, String reference, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }
        Wallet wallet = walletRepository.findByOrganizationIdForUpdate(organizationId)
                .orElseGet(() -> {
                    ensureWallet(organizationId);
                    return walletRepository.findByOrganizationIdForUpdate(organizationId)
                            .orElseThrow(() -> new ApiException("Wallet not found", HttpStatus.NOT_FOUND));
                });

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);
        wallet.setBalance(after);
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .organization(wallet.getOrganization())
                .wallet(wallet)
                .type(WalletTransactionType.ADJUSTMENT)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .reference(reference)
                .description(description)
                .build());

        orgNotificationService.notifyTopUpSuccess(wallet.getOrganization(), amount, after, null);
    }

    @Transactional
    public void refund(UUID organizationId, BigDecimal amount, String reference, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (reference != null && walletTransactionRepository.findByReference(reference).isPresent()) {
            log.info("Skipping duplicate wallet refund reference={}", reference);
            return;
        }

        Wallet wallet = walletRepository.findByOrganizationIdForUpdate(organizationId)
                .orElseThrow(() -> new ApiException("Wallet not found", HttpStatus.NOT_FOUND));

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);
        wallet.setBalance(after);
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .organization(wallet.getOrganization())
                .wallet(wallet)
                .type(WalletTransactionType.REFUND)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .reference(reference)
                .description(description)
                .build());
    }

    @Transactional
    public void assertSufficientBalance(UUID organizationId, int messageCount) {
        BigDecimal required = billingSettingsService.customerPrice().multiply(BigDecimal.valueOf(messageCount));
        assertSufficientAmount(organizationId, required);
    }

    @Transactional
    public void assertSufficientAmount(UUID organizationId, BigDecimal required) {
        Wallet wallet = walletRepository.findByOrganizationIdForUpdate(organizationId)
                .orElseGet(() -> {
                    ensureWallet(organizationId);
                    return walletRepository.findByOrganizationIdForUpdate(organizationId)
                            .orElseThrow(() -> new ApiException("Wallet not found", HttpStatus.NOT_FOUND));
                });
        if (required == null) {
            required = BigDecimal.ZERO;
        }
        if (wallet.getBalance().compareTo(required) < 0) {
            throw insufficient(required, wallet);
        }
    }

    @Transactional
    public void debitForSms(UUID organizationId, BigDecimal amount, String reference, String description) {
        if (reference != null && walletTransactionRepository.findByReference(reference).isPresent()) {
            log.info("Skipping duplicate wallet debit reference={}", reference);
            return;
        }
        Wallet wallet = walletRepository.findByOrganizationIdForUpdate(organizationId)
                .orElseThrow(() -> new ApiException("Wallet not found", HttpStatus.NOT_FOUND));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw insufficient(amount, wallet);
        }

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.subtract(amount);
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw insufficient(amount, wallet);
        }
        wallet.setBalance(after);
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .organization(wallet.getOrganization())
                .wallet(wallet)
                .type(WalletTransactionType.SMS_DEBIT)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .reference(reference)
                .description(description)
                .build());

        if (orgNotificationService.crossedLowBalanceThreshold(wallet.getOrganization(), before, after)) {
            orgNotificationService.notifyLowBalance(wallet.getOrganization(), after);
        }
    }

    private ApiException insufficient(BigDecimal required, Wallet wallet) {
        BigDecimal need = required == null ? BigDecimal.ZERO : required.setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal available = wallet.getBalance() == null
                ? BigDecimal.ZERO
                : wallet.getBalance().setScale(2, java.math.RoundingMode.HALF_UP);
        String currency = wallet.getCurrency() != null ? wallet.getCurrency() : billingSettingsService.currency();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("required", need);
        data.put("available", available);
        data.put("currency", currency);
        return new ApiException("Insufficient wallet balance", HttpStatus.PAYMENT_REQUIRED, data);
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> history(
            UUID organizationId,
            WalletTransactionType type,
            List<TopupStatus> statuses,
            Pageable pageable) {

        getOrganization(organizationId);

        boolean statusesEmpty = statuses == null || statuses.isEmpty();
        Collection<TopupStatus> statusFilter = statusesEmpty
                ? List.of(TopupStatus.PENDING)
                : statuses;

        return walletTransactionRepository
                .findByOrganizationFiltered(organizationId, type, statusFilter, statusesEmpty, pageable)
                .map(tx -> toTransactionResponse(tx, organizationId));
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> platformHistory(
            WalletTransactionType type,
            List<TopupStatus> statuses,
            Pageable pageable) {
        boolean statusesEmpty = statuses == null || statuses.isEmpty();
        Collection<TopupStatus> statusFilter = statusesEmpty
                ? List.of(TopupStatus.PENDING)
                : statuses;

        return walletTransactionRepository
                .findPlatformFiltered(type, statusFilter, statusesEmpty, pageable)
                .map(tx -> toTransactionResponse(tx, organizationIdOf(tx)));
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction tx, UUID organizationId) {
        Organization org = initializedOrganization(tx);
        String account = tx.getBillRef();
        if (account == null || account.isBlank()) {
            account = org != null ? org.getMpesaAccountRef() : null;
        }
        return WalletTransactionResponse.builder()
                .id(tx.getId())
                .organizationId(organizationId)
                .type(tx.getType())
                .amount(tx.getAmount())
                .balanceBefore(tx.getBalanceBefore())
                .balanceAfter(tx.getBalanceAfter())
                .reference(tx.getReference())
                .description(tx.getDescription())
                .mpesaReceipt(tx.getMpesaReceipt())
                .phoneNumber(tx.getPhoneNumber())
                .checkoutRequestId(tx.getCheckoutRequestId())
                .status(tx.getTopupStatus())
                .resultCode(tx.getResultCode())
                .resultDesc(tx.getResultDesc())
                .callbackReceived(tx.isCallbackReceived())
                .walletCredited(tx.isWalletCredited())
                .paymentMethod(resolvePaymentMethod(tx))
                .paybill(tx.getType() == WalletTransactionType.TOPUP ? appProperties.getMpesa().getShortcode() : null)
                .accountNumber(tx.getType() == WalletTransactionType.TOPUP ? account : null)
                .organizationName(org != null ? org.getName() : null)
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private Organization initializedOrganization(WalletTransaction tx) {
        Organization org = tx.getOrganization();
        if (org == null || !Hibernate.isInitialized(org)) {
            return null;
        }
        return org;
    }

    private UUID organizationIdOf(WalletTransaction tx) {
        Organization org = tx.getOrganization();
        return org == null ? null : org.getId();
    }

    private PaymentMethod resolvePaymentMethod(WalletTransaction tx) {
        if (tx.getType() != WalletTransactionType.TOPUP) {
            return null;
        }
        if (tx.getPaymentMethod() != null) {
            return tx.getPaymentMethod();
        }
        if (tx.getCheckoutRequestId() != null && !tx.getCheckoutRequestId().isBlank()) {
            return PaymentMethod.STK_PUSH;
        }
        return PaymentMethod.PAYBILL;
    }

    private static final int MPESA_ACCOUNT_REF_LENGTH = 8;

    public String buildAccountReference(UUID organizationId) {
        return buildAccountReference(organizationId, 0);
    }

    @Transactional
    public Organization ensureMpesaAccountRef(Organization org) {
        String current = org.getMpesaAccountRef();
        if (isShortPaybillAccount(current) && !accountRefTakenByOther(current, org.getId())) {
            return org;
        }
        if (current != null && !current.isBlank()) {
            String shortened = current.trim().toUpperCase();
            if (shortened.length() > MPESA_ACCOUNT_REF_LENGTH) {
                shortened = shortened.substring(0, MPESA_ACCOUNT_REF_LENGTH);
            }
            if (isShortPaybillAccount(shortened) && !accountRefTakenByOther(shortened, org.getId())) {
                org.setMpesaAccountRef(shortened);
                return organizationRepository.save(org);
            }
        }
        org.setMpesaAccountRef(allocateUniqueAccountRef(org.getId()));
        return organizationRepository.save(org);
    }

    private UUID resolveOrganizationFromBillRef(String billRef) {
        if (billRef == null || billRef.isBlank()) {
            return null;
        }
        String normalized = billRef.trim().toUpperCase();
        Optional<Organization> exact = organizationRepository.findByMpesaAccountRefIgnoreCase(normalized);
        if (exact.isPresent()) {
            return exact.get().getId();
        }
        if (normalized.length() > MPESA_ACCOUNT_REF_LENGTH) {
            return organizationRepository.findByMpesaAccountRefIgnoreCase(normalized.substring(0, MPESA_ACCOUNT_REF_LENGTH))
                    .map(Organization::getId)
                    .orElse(null);
        }
        return null;
    }

    private String buildAccountReference(UUID organizationId, int suffixOffset) {
        String prefix = accountRefPrefix();
        int suffixLength = MPESA_ACCOUNT_REF_LENGTH - prefix.length();
        String compact = organizationId.toString().replace("-", "").toUpperCase();
        int start = Math.max(0, suffixOffset);
        if (start + suffixLength > compact.length()) {
            start = 0;
        }
        return prefix + compact.substring(start, start + suffixLength);
    }

    private String allocateUniqueAccountRef(UUID organizationId) {
        String prefix = accountRefPrefix();
        int suffixLength = MPESA_ACCOUNT_REF_LENGTH - prefix.length();
        String compact = organizationId.toString().replace("-", "").toUpperCase();
        int maxOffset = Math.max(0, compact.length() - suffixLength);
        for (int offset = 0; offset <= maxOffset; offset++) {
            String candidate = buildAccountReference(organizationId, offset);
            if (!accountRefTakenByOther(candidate, organizationId)) {
                return candidate;
            }
        }
        for (int n = 0; n < 36 * 36; n++) {
            String suffix = Integer.toString(n, 36).toUpperCase();
            while (suffix.length() < suffixLength) {
                suffix = "0" + suffix;
            }
            if (suffix.length() > suffixLength) {
                suffix = suffix.substring(suffix.length() - suffixLength);
            }
            String candidate = prefix + suffix;
            if (!accountRefTakenByOther(candidate, organizationId)) {
                return candidate;
            }
        }
        throw new ApiException("Could not allocate a unique M-Pesa account number", HttpStatus.CONFLICT);
    }

    private boolean isShortPaybillAccount(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String ref = value.trim().toUpperCase();
        return ref.length() == MPESA_ACCOUNT_REF_LENGTH && ref.matches("[A-Z0-9]+");
    }

    private boolean accountRefTakenByOther(String accountRef, UUID organizationId) {
        return organizationRepository.findByMpesaAccountRefIgnoreCase(accountRef)
                .filter(existing -> organizationId == null || !existing.getId().equals(organizationId))
                .isPresent();
    }

    private String accountRefPrefix() {
        String prefix = appProperties.getMpesa().getAccountReferencePrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = "NOVA";
        }
        prefix = prefix.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (prefix.length() > 4) {
            prefix = prefix.substring(0, 4);
        }
        if (prefix.isEmpty()) {
            prefix = "NOVA";
        }
        return prefix;
    }

    private String extractCallbackMetadata(JsonNode stkCallback, String name) {
        JsonNode items = stkCallback.path("CallbackMetadata").path("Item");
        if (!items.isArray()) {
            return null;
        }
        for (JsonNode item : items) {
            if (name.equalsIgnoreCase(item.path("Name").asText())) {
                JsonNode value = item.get("Value");
                return value == null || value.isNull() ? null : value.asText();
            }
        }
        return null;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return value == null || value.isBlank() || "null".equalsIgnoreCase(value) ? null : value;
    }

    private String normalizePhone(String phone) {
        String cleaned = phone.replaceAll("[\\s\\-()]", "");
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("0") && cleaned.length() == 10) {
            cleaned = "254" + cleaned.substring(1);
        }
        if (cleaned.contains(".")) {
            cleaned = cleaned.substring(0, cleaned.indexOf('.'));
        }
        if (!cleaned.matches("254\\d{9}")) {
            throw new ApiException("Phone must be a valid Kenyan M-Pesa number (2547XXXXXXXX)", HttpStatus.BAD_REQUEST);
        }
        return cleaned;
    }

    private String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public Map<String, String> c2bCallbackUrls() {
        String base = trimSlash(appProperties.getMpesa().getCallbackBaseUrl());
        Map<String, String> body = new LinkedHashMap<>();
        body.put("shortcode", appProperties.getMpesa().getShortcode());
        body.put("responseType", "Completed");
        body.put("confirmationUrl", base + "/api/v1/payments/c2b/confirmation");
        body.put("validationUrl", base + "/api/v1/payments/c2b/validation");
        return body;
    }

    public Map<String, String> registerC2bV2Urls() {
        Map<String, String> body = c2bCallbackUrls();
        String confirmationUrl = body.get("confirmationUrl");
        String validationUrl = body.get("validationUrl");
        assertSafeCallbackUrl(confirmationUrl);
        assertSafeCallbackUrl(validationUrl);
        MpesaDarajaClient.C2bRegisterResult result = mpesaDarajaClient.registerC2bUrls(confirmationUrl, validationUrl);
        body.put("success", Boolean.toString(result.success()));
        body.put("alreadyRegistered", Boolean.toString(result.alreadyRegistered()));
        body.put("errorCode", result.errorCode() == null ? "" : result.errorCode());
        body.put("message", result.message() == null ? "" : result.message());
        body.put("darajaResponse", result.rawResponse() == null ? "" : result.rawResponse());
        return body;
    }

    private void assertSafeCallbackUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new ApiException("Callback URL is missing. Set novastack.mpesa.callback-base-url.",
                    HttpStatus.BAD_REQUEST);
        }
        String lower = url.toLowerCase(java.util.Locale.ROOT);
        if (!lower.startsWith("https://")) {
            throw new ApiException("Production C2B URLs must be HTTPS: " + url, HttpStatus.BAD_REQUEST);
        }
        if (lower.contains("ngrok") || lower.contains("mockbin") || lower.contains("requestbin")
                || lower.contains("localhost") || lower.contains("127.0.0.1")) {
            throw new ApiException("C2B production URLs cannot use localhost or public URL testers: " + url,
                    HttpStatus.BAD_REQUEST);
        }
        String path = lower.replace("https://", "");
        int slash = path.indexOf('/');
        String afterHost = slash < 0 ? "" : path.substring(slash);
        if (afterHost.contains("m-pesa") || afterHost.contains("mpesa") || afterHost.contains("safaricom")
                || afterHost.contains("/exe") || afterHost.contains("exec") || afterHost.contains("/cmd")
                || afterHost.contains("/sql") || afterHost.contains("query")) {
            throw new ApiException(
                    "Callback path cannot contain mpesa, safaricom, exe, exec, cmd, sql, or query: " + url,
                    HttpStatus.BAD_REQUEST);
        }
    }

    private JsonNode unwrapCallback(JsonNode payload) {
        if (payload == null || payload.isMissingNode() || payload.isNull()) {
            return null;
        }
        if (payload.hasNonNull("TransID") || payload.hasNonNull("TransAmount") || payload.hasNonNull("BillRefNumber")) {
            return payload;
        }
        if (payload.has("Body") && payload.get("Body").isObject()) {
            return payload.get("Body");
        }
        return payload;
    }

    private BigDecimal parseKes(String amountStr) {
        String cleaned = amountStr.replace(",", "").trim();
        BigDecimal amount = new BigDecimal(cleaned);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return amount.setScale(4, RoundingMode.HALF_UP);
    }

    private String jsonText(JsonNode payload, String... fields) {
        for (String field : fields) {
            String value = textOrNull(payload.path(field));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String safePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        try {
            return normalizePhone(phone);
        } catch (ApiException ex) {
            String trimmed = phone.trim();
            if (trimmed.length() > PHONE_COLUMN_LENGTH) {
                log.warn("C2B MSISDN length {} exceeds phone_number column; truncating", trimmed.length());
                return trimmed.substring(0, PHONE_COLUMN_LENGTH);
            }
            return trimmed;
        }
    }

    private boolean isPlainMsisdn(String phone) {
        return phone != null && phone.matches("254\\d{9}");
    }

    private void logWalletTransactionSave(WalletTransaction tx, String rawMsisdn) {
        log.info(
                "Saving wallet transaction phoneNumber={} checkoutRequestId={} mpesaReceipt={} billRef={} rawMsisdnLength={}",
                tx.getPhoneNumber(),
                tx.getCheckoutRequestId(),
                tx.getMpesaReceipt(),
                tx.getBillRef(),
                rawMsisdn == null ? 0 : rawMsisdn.length());
    }

    /**
     * Notifications and "credited" logs must not run if the DB commit rolls back.
     * Unit tests have no transaction, so they run the action immediately.
     */
    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private Wallet getWallet(UUID organizationId) {
        return ensureWallet(organizationId);
    }

    private Organization getOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));
    }
}
