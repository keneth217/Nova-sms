package com.novastack.sms.controller;

import com.novastack.sms.domain.enums.TopupStatus;
import com.novastack.sms.domain.enums.UserRole;
import com.novastack.sms.domain.enums.WalletTransactionType;
import com.novastack.sms.dto.request.VerifyMpesaReceiptRequest;
import com.novastack.sms.dto.request.WalletTopupRequest;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.dto.response.MpesaReceiptLookupResponse;
import com.novastack.sms.dto.response.StkPushResponse;
import com.novastack.sms.dto.response.WalletBalanceResponse;
import com.novastack.sms.dto.response.WalletTransactionResponse;
import com.novastack.sms.security.SecurityUtils;
import com.novastack.sms.security.UserPrincipal;
import com.novastack.sms.service.IdempotencyService;
import com.novastack.sms.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet")
public class WalletController {

    private final WalletService walletService;
    private final IdempotencyService idempotencyService;

    @GetMapping("/balance")
    @Operation(
            summary = "Get wallet balance",
            description = "Dashboard JWT or scoped API key with WALLET_READ. Returns the organization wallet so partner apps can show balance on their own site.")
    public ApiResponse<WalletBalanceResponse> balance() {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(walletService.getBalance(orgId));
    }

    @PostMapping("/topup")
    @Operation(summary = "Top up wallet via M-Pesa Daraja STK Push to platform Paybill",
            description = """
      Triggers Lipa Na M-Pesa STK on the user's phone. Internally the same as POST /api/v1/mpesa/stkpush.
      Wallet is credited after Nova receives the Safaricom callback — clients do not implement that callback.
      Poll the returned transactionId. Optional Idempotency-Key (for example learner-839-payment-2026-08-24)
      replays the original STK if the HTTP client times out.
                    """)
    @Parameter(name = MpesaController.IDEMPOTENCY_HEADER, in = ParameterIn.HEADER, required = false,
            description = "Unique request id. Reuse with the same body to avoid a duplicate STK Push.")
    public ApiResponse<StkPushResponse> topup(
            @Valid @RequestBody WalletTopupRequest request,
            HttpServletRequest http) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        UUID clientId = SecurityUtils.optionalApiClientId().orElse(null);
        String idempotencyKey = http.getHeader(MpesaController.IDEMPOTENCY_HEADER);
        String hash = MpesaController.stkRequestHash(request);
        StkPushResponse data = idempotencyService.replayOrRun(
                clientId,
                idempotencyKey,
                hash,
                IdempotencyService.TYPE_STK,
                () -> {
                    StkPushResponse sent = walletService.initiateTopUp(orgId, request);
                    return new IdempotencyService.ReplayResult<>(sent, sent.getTransactionId());
                },
                resourceId -> walletService.getTopUpStatus(orgId, resourceId));
        return ApiResponse.ok("STK Push sent. Enter M-Pesa PIN on your phone.", data);
    }

    @GetMapping("/topup/{transactionId}")
    @Operation(summary = "Read top-up status from database (does not call Safaricom)",
            description = "Requires WALLET_TOPUP for scoped API clients.")
    public ApiResponse<StkPushResponse> topupStatus(@PathVariable UUID transactionId) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(walletService.getTopUpStatus(orgId, transactionId));
    }

    @PostMapping("/topup/verify-receipt")
    @Operation(
            summary = "Look up a Paybill or STK top-up by M-Pesa receipt",
            description = """
                    Checks Nova for the receipt. If the original C2B callback was stored
                    (BillRefNumber + amount), the organization is resolved from that account
                    and credited automatically when it matches this wallet.
                    Requires WALLET_TOPUP for scoped API clients.
                    """)
    public ApiResponse<MpesaReceiptLookupResponse> verifyReceipt(@Valid @RequestBody VerifyMpesaReceiptRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(walletService.verifyReceipt(orgId, request.getMpesaReceipt()));
    }

    @PostMapping("/topup/{transactionId}/check")
    @Operation(
            summary = "Check transaction: poll DB for Safaricom callback, and if still PENDING query Daraja then update DB",
            description = """
                    1) Reads wallet_transactions from MySQL.
                    2) If COMPLETED/FAILED — returns that status (never moves backwards).
                    3) If PENDING — queries Safaricom STK status API and updates the database.
                       "Still under processing" stays PENDING. Wallet is credited only on ResultCode 0
                       (query or callback). The Safaricom callback remains the authoritative confirmation.
                    Poll this every 3–5 seconds after initiating top-up.
                    Requires WALLET_TOPUP for scoped API clients.
                    """)
    public ApiResponse<StkPushResponse> checkTopup(@PathVariable UUID transactionId) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(walletService.checkTopUpTransaction(orgId, transactionId));
    }

    @GetMapping("/transactions")
    @Operation(
            summary = "List organization wallet transactions",
            description = """
                    Lists transactions for the authenticated organization (or organizationId for SUPER_ADMIN).
                    Filter by top-up status: PENDING, COMPLETED, FAILED (repeatable: status=PENDING&status=COMPLETED).
                    Filter by type: TOPUP, SMS_DEBIT, REFUND, ADJUSTMENT.
                    Scoped API keys with WALLET_READ only see their own organization.
                    """)
    public ApiResponse<Page<WalletTransactionResponse>> transactions(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) WalletTransactionType type,
            @RequestParam(required = false) List<TopupStatus> status,
            @PageableDefault(size = 20) Pageable pageable) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (organizationId == null
                && principal.getRole() == UserRole.SUPER_ADMIN
                && principal.getOrganizationId() == null) {
            return ApiResponse.ok(walletService.platformHistory(type, status, pageable));
        }
        UUID orgId = SecurityUtils.resolveOrganizationId(organizationId);
        return ApiResponse.ok(walletService.history(orgId, type, status, pageable));
    }
}
