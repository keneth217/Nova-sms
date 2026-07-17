package com.novastack.sms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.entity.Wallet;
import com.novastack.sms.domain.entity.WalletTransaction;
import com.novastack.sms.domain.enums.TopupStatus;
import com.novastack.sms.domain.enums.WalletTransactionType;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.domain.repository.WalletRepository;
import com.novastack.sms.domain.repository.WalletTransactionRepository;
import com.novastack.sms.dto.request.WalletTopupRequest;
import com.novastack.sms.dto.response.StkPushResponse;
import com.novastack.sms.dto.response.WalletBalanceResponse;
import com.novastack.sms.dto.response.WalletTransactionResponse;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.mpesa.MpesaDarajaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final OrganizationRepository organizationRepository;
    private final AppProperties appProperties;
    private final MpesaDarajaClient mpesaDarajaClient;

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
        Organization org = getOrganization(organizationId);
        Wallet wallet = ensureWallet(organizationId);
        return WalletBalanceResponse.builder()
                .walletId(wallet.getId())
                .organizationId(organizationId)
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .smsCost(org.getSmsCost())
                .build();
    }

    /**
     * Initiates M-Pesa Daraja STK Push to the configured Paybill.
     * Wallet is credited only after a successful callback.
     */
    @Transactional
    public StkPushResponse initiateTopUp(UUID organizationId, WalletTopupRequest request) {
        Organization org = getOrganization(organizationId);
        if (org.getMpesaAccountRef() == null || org.getMpesaAccountRef().isBlank()) {
            org.setMpesaAccountRef(buildAccountReference(organizationId));
            organizationRepository.save(org);
        }
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

        log.info("STK Push initiated org={} checkoutRequestId={} amount={}",
                organizationId, stk.checkoutRequestId(), request.getAmount());

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
        log.info("Processing STK callback payload={}", payload);
        JsonNode body = payload.path("Body").path("stkCallback");
        if (body.isMissingNode()) {
            log.warn("Invalid STK callback payload: {}", payload);
            return;
        }

        String checkoutRequestId = body.path("CheckoutRequestID").asText(null);
        String resultCode = body.path("ResultCode").asText();
        String resultDesc = body.path("ResultDesc").asText();

        if (checkoutRequestId == null || checkoutRequestId.isBlank()) {
            log.warn("STK callback missing CheckoutRequestID");
            return;
        }

        WalletTransaction tx = walletTransactionRepository.findByCheckoutRequestId(checkoutRequestId)
                .orElse(null);
        if (tx == null) {
            log.warn("No pending top-up for CheckoutRequestID={}", checkoutRequestId);
            return;
        }

        if (tx.getTopupStatus() == TopupStatus.COMPLETED || tx.getTopupStatus() == TopupStatus.FAILED) {
            log.info("Ignoring duplicate STK callback for {}", checkoutRequestId);
            return;
        }

        if (!"0".equals(resultCode)) {
            markTopUpFailed(tx, resultCode, resultDesc);
            return;
        }

        String mpesaReceipt = extractCallbackMetadata(body, "MpesaReceiptNumber");
        String amountStr = extractCallbackMetadata(body, "Amount");
        String phone = extractCallbackMetadata(body, "PhoneNumber");
        BigDecimal paidAmount = amountStr != null ? new BigDecimal(amountStr) : tx.getAmount();

        completeSuccessfulTopUp(tx, paidAmount, mpesaReceipt, phone, resultCode, resultDesc);
    }

    /**
     * Poll our DB for callback status. If still PENDING, query Safaricom STK status and update DB.
     */
    @Transactional
    public StkPushResponse checkTopUpTransaction(UUID organizationId, UUID transactionId) {
        WalletTransaction tx = walletTransactionRepository.findByIdAndOrganizationId(transactionId, organizationId)
                .orElseThrow(() -> new ApiException("Top-up transaction not found", HttpStatus.NOT_FOUND));

        if (tx.getTopupStatus() == TopupStatus.COMPLETED || tx.getTopupStatus() == TopupStatus.FAILED) {
            return toStkResponse(tx, "Transaction already finalized in database");
        }

        if (tx.getCheckoutRequestId() == null || tx.getCheckoutRequestId().isBlank()) {
            return toStkResponse(tx, "Waiting for STK initiation to complete");
        }

        // Re-read from DB in case callback arrived between request and lock
        tx = walletTransactionRepository.findByCheckoutRequestId(tx.getCheckoutRequestId())
                .orElse(tx);
        if (tx.getTopupStatus() == TopupStatus.COMPLETED || tx.getTopupStatus() == TopupStatus.FAILED) {
            return toStkResponse(tx, "Callback already received and database updated");
        }

        log.info("Polling Safaricom STK status for checkoutRequestId={}", tx.getCheckoutRequestId());
        MpesaDarajaClient.StkQueryResult query = mpesaDarajaClient.queryStkStatus(tx.getCheckoutRequestId());

        if (query.isPaymentSuccessful()) {
            completeSuccessfulTopUp(
                    tx,
                    tx.getAmount(),
                    tx.getMpesaReceipt(),
                    tx.getPhoneNumber(),
                    query.resultCode(),
                    query.resultDesc() != null ? query.resultDesc() : "Confirmed via STK query"
            );
            return toStkResponse(
                    walletTransactionRepository.findById(tx.getId()).orElse(tx),
                    "Payment confirmed via Safaricom query; wallet updated"
            );
        }

        if (query.isTerminalFailure()) {
            markTopUpFailed(tx, query.resultCode(), query.resultDesc());
            return toStkResponse(
                    walletTransactionRepository.findById(tx.getId()).orElse(tx),
                    "Payment failed/cancelled; database updated"
            );
        }

        tx.setResultCode(query.resultCode());
        tx.setResultDesc(query.resultDesc() != null ? query.resultDesc() : "Awaiting Safaricom callback");
        walletTransactionRepository.save(tx);

        return toStkResponse(tx, "Still pending — waiting for Safaricom callback or user PIN entry");
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
            String resultCode,
            String resultDesc) {

        if (tx.getTopupStatus() == TopupStatus.COMPLETED) {
            return;
        }

        if (mpesaReceipt != null && walletTransactionRepository.findByReference(mpesaReceipt)
                .filter(existing -> !existing.getId().equals(tx.getId()))
                .isPresent()) {
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
        tx.setMpesaReceipt(mpesaReceipt);
        if (phone != null && !phone.isBlank()) {
            try {
                tx.setPhoneNumber(normalizePhone(phone));
            } catch (ApiException ignored) {
                tx.setPhoneNumber(phone);
            }
        }
        if (mpesaReceipt != null && !mpesaReceipt.isBlank()) {
            tx.setReference(mpesaReceipt);
        }
        tx.setResultCode(resultCode);
        tx.setResultDesc(resultDesc);
        tx.setTopupStatus(TopupStatus.COMPLETED);
        tx.setDescription("M-Pesa Paybill " + appProperties.getMpesa().getShortcode()
                + " top-up" + (mpesaReceipt != null ? " receipt " + mpesaReceipt : ""));
        walletTransactionRepository.save(tx);

        log.info("Wallet topped up org={} amount={} receipt={}",
                tx.getOrganization().getId(), paidAmount, mpesaReceipt);
    }

    private void markTopUpFailed(WalletTransaction tx, String resultCode, String resultDesc) {
        tx.setResultCode(resultCode);
        tx.setResultDesc(resultDesc);
        tx.setTopupStatus(TopupStatus.FAILED);
        walletTransactionRepository.save(tx);
        log.info("STK payment failed checkoutRequestId={} desc={}", tx.getCheckoutRequestId(), resultDesc);
    }

    private StkPushResponse toStkResponse(WalletTransaction tx, String message) {
        boolean finalized = tx.getTopupStatus() == TopupStatus.COMPLETED || tx.getTopupStatus() == TopupStatus.FAILED;
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
                .callbackReceived(finalized)
                .walletCredited(tx.getTopupStatus() == TopupStatus.COMPLETED)
                .updatedAt(tx.getCreatedAt())
                .build();
    }

    /**
     * C2B Paybill confirmation: customer paid via M-Pesa to Paybill with BillRefNumber = org account ref.
     */
    @Transactional
    public Map<String, Object> handleC2bConfirmation(Map<String, String> payload) {
        String receipt = payload.getOrDefault("TransID", payload.get("transactionId"));
        String amountStr = payload.getOrDefault("TransAmount", payload.get("amount"));
        String billRef = payload.getOrDefault("BillRefNumber", payload.get("accountReference"));
        String phone = payload.getOrDefault("MSISDN", payload.get("phoneNumber"));

        if (receipt == null || amountStr == null || billRef == null) {
            return Map.of("ResultCode", 1, "ResultDesc", "Missing required fields");
        }

        if (walletTransactionRepository.findByReference(receipt).isPresent()) {
            return Map.of("ResultCode", 0, "ResultDesc", "Already processed");
        }

        UUID organizationId = resolveOrganizationFromBillRef(billRef);
        if (organizationId == null) {
            log.warn("Unknown BillRefNumber for C2B: {}", billRef);
            return Map.of("ResultCode", 1, "ResultDesc", "Invalid account reference");
        }

        BigDecimal amount = new BigDecimal(amountStr);
        Wallet wallet = walletRepository.findByOrganizationIdForUpdate(organizationId)
                .orElseThrow(() -> new ApiException("Wallet not found", HttpStatus.NOT_FOUND));

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);
        wallet.setBalance(after);
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .organization(wallet.getOrganization())
                .wallet(wallet)
                .type(WalletTransactionType.TOPUP)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .reference(receipt)
                .mpesaReceipt(receipt)
                .phoneNumber(phone != null ? normalizePhone(phone) : null)
                .topupStatus(TopupStatus.COMPLETED)
                .description("M-Pesa C2B Paybill top-up via account " + billRef)
                .build());

        return Map.of("ResultCode", 0, "ResultDesc", "Accepted");
    }

    @Transactional
    public void debitForSms(UUID organizationId, BigDecimal amount, String reference, String description) {
        Wallet wallet = walletRepository.findByOrganizationIdForUpdate(organizationId)
                .orElseThrow(() -> new ApiException("Wallet not found", HttpStatus.NOT_FOUND));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new ApiException("Insufficient wallet balance", HttpStatus.PAYMENT_REQUIRED);
        }

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.subtract(amount);
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
    }

    @Transactional
    public void refund(UUID organizationId, BigDecimal amount, String reference, String description) {
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
        Organization org = getOrganization(organizationId);
        Wallet wallet = ensureWallet(organizationId);
        BigDecimal required = org.getSmsCost().multiply(BigDecimal.valueOf(messageCount));
        if (wallet.getBalance().compareTo(required) < 0) {
            throw new ApiException(
                    "Insufficient wallet balance. Required: " + required + " " + wallet.getCurrency(),
                    HttpStatus.PAYMENT_REQUIRED);
        }
    }

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
                .map(tx -> toTransactionResponse(tx, tx.getOrganization().getId()));
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction tx, UUID organizationId) {
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
                .createdAt(tx.getCreatedAt())
                .build();
    }

    public String buildAccountReference(UUID organizationId) {
        String prefix = appProperties.getMpesa().getAccountReferencePrefix();
        String compact = organizationId.toString().replace("-", "");
        return (prefix + compact.substring(0, Math.min(8, compact.length()))).toUpperCase();
    }

    private UUID resolveOrganizationFromBillRef(String billRef) {
        if (billRef == null || billRef.isBlank()) {
            return null;
        }
        return organizationRepository.findByMpesaAccountRefIgnoreCase(billRef.trim())
                .map(Organization::getId)
                .orElse(null);
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

    private Wallet getWallet(UUID organizationId) {
        return ensureWallet(organizationId);
    }

    private Organization getOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));
    }
}
