package com.novastack.sms.controller;

import com.novastack.sms.dto.request.VerifyMpesaReceiptRequest;
import com.novastack.sms.dto.request.WalletTopupRequest;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.dto.response.MpesaC2bInstructionsResponse;
import com.novastack.sms.dto.response.MpesaReceiptLookupResponse;
import com.novastack.sms.dto.response.StkPushResponse;
import com.novastack.sms.dto.response.WalletTransactionResponse;
import com.novastack.sms.security.SecurityUtils;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mpesa")
@RequiredArgsConstructor
@Tag(
        name = "M-Pesa",
        description = "Client STK, checkout, and Paybill C2B. Clients do not configure or implement Safaricom callbacks.")
public class MpesaController {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final WalletService walletService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/stkpush")
    @Operation(
            summary = "Initiate an M-Pesa STK Push",
            description = """
                    Requires MPESA_STK_PUSH (or WALLET_TOPUP). Optional Idempotency-Key (for example \
                    learner-839-payment-2026-08-24) replays the original transaction instead of sending a duplicate STK. \
                    Clients do not receive the Safaricom callback — poll GET /transactions/{id}/status.
                    """)
    @Parameter(name = IDEMPOTENCY_HEADER, in = ParameterIn.HEADER, required = false,
            description = "Unique request id. Reuse with the same body to avoid a duplicate STK Push.")
    public ApiResponse<StkPushResponse> stkPush(
            @Valid @RequestBody WalletTopupRequest request,
            HttpServletRequest http) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        UUID clientId = SecurityUtils.optionalApiClientId().orElse(null);
        String idempotencyKey = http.getHeader(IDEMPOTENCY_HEADER);
        String hash = stkRequestHash(request);
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

    @PostMapping("/checkout")
    @Operation(
            summary = "Lipa Na M-Pesa Online Checkout (same as STK Push)",
            description = "Alias of POST /stkpush. Safaricom CheckoutRequestID is stored on the transaction. "
                    + "Requires MPESA_STK_PUSH (or WALLET_TOPUP). Optional Idempotency-Key.")
    @Parameter(name = IDEMPOTENCY_HEADER, in = ParameterIn.HEADER, required = false)
    public ApiResponse<StkPushResponse> checkout(
            @Valid @RequestBody WalletTopupRequest request,
            HttpServletRequest http) {
        return stkPush(request, http);
    }

    @GetMapping("/checkout/{id}")
    @Operation(summary = "Get a checkout / STK transaction (does not call Safaricom)")
    public ApiResponse<StkPushResponse> getCheckout(@PathVariable UUID id) {
        return getTransaction(id);
    }

    @GetMapping("/checkout/{id}/status")
    @Operation(summary = "Refresh checkout / STK status from Nova, querying Safaricom if still PENDING")
    public ApiResponse<StkPushResponse> checkoutStatus(@PathVariable UUID id) {
        return status(id);
    }

    @GetMapping("/c2b/transactions")
    @Operation(
            summary = "List Paybill C2B wallet credits for this organization",
            description = """
                    Authenticated status for C2B payments Nova already recorded. Safaricom confirmation is handled \
                    internally. Requires MPESA_C2B (or WALLET_READ / WALLET_TOPUP). \
                    C2B URL registration with Daraja is Super Admin only.
                    """)
    public ApiResponse<Page<WalletTransactionResponse>> listC2bTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(walletService.listC2bTransactions(orgId, pageable));
    }

    @GetMapping("/c2b/transactions/{id}")
    @Operation(
            summary = "Get one Paybill C2B transaction",
            description = "Requires MPESA_C2B (or WALLET_READ / WALLET_TOPUP). Returns 404 if the id is not a C2B top-up.")
    public ApiResponse<WalletTransactionResponse> getC2bTransaction(@PathVariable UUID id) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(walletService.getC2bTransaction(orgId, id));
    }

    @GetMapping("/c2b")
    @Operation(
            summary = "Paybill C2B instructions for this organization",
            description = """
                    Returns the platform Paybill and this organization's account number.
                    Show these to the customer. Safaricom C2B confirmation is posted to Nova SMS,
                    not to your application. Requires MPESA_C2B (or WALLET_READ / WALLET_TOPUP).
                    """)
    public ApiResponse<MpesaC2bInstructionsResponse> c2bInstructions() {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(walletService.c2bInstructions(orgId));
    }

    @PostMapping("/c2b/verify")
    @Operation(
            summary = "Look up a Paybill C2B (or STK) top-up by M-Pesa receipt",
            description = """
                    Use this after the customer pays Paybill. Nova matches the stored C2B confirmation,
                    or asks Safaricom Transaction Status internally if the callback is delayed.
                    Clients never call Daraja. Requires MPESA_C2B (or WALLET_READ / WALLET_TOPUP).
                    """)
    public ApiResponse<MpesaReceiptLookupResponse> verifyC2b(
            @Valid @RequestBody VerifyMpesaReceiptRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(walletService.verifyReceipt(orgId, request.getMpesaReceipt()));
    }

    @GetMapping("/transactions/{id}")
    @Operation(summary = "Get an STK transaction from the database (does not call Safaricom)",
            description = "Requires MPESA_STATUS (or WALLET_TOPUP / MPESA_STK_PUSH) for scoped API clients.")
    public ApiResponse<StkPushResponse> getTransaction(@PathVariable UUID id) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(walletService.getTopUpStatus(orgId, id));
    }

    @GetMapping("/transactions/{id}/status")
    @Operation(
            summary = "Refresh STK transaction status",
            description = """
                    Reads the stored row; if still PENDING, queries Safaricom and updates the database.
                    Requires MPESA_STATUS (or WALLET_TOPUP / MPESA_STK_PUSH) for scoped API clients.
                    Poll this until status is COMPLETED and walletCredited is true, or FAILED.
                    """)
    public ApiResponse<StkPushResponse> status(@PathVariable UUID id) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(walletService.checkTopUpTransaction(orgId, id));
    }

    static String stkRequestHash(WalletTopupRequest request) {
        String amount = request.getAmount() == null ? "" : request.getAmount().stripTrailingZeros().toPlainString();
        String phone = request.getPhoneNumber() == null ? "" : request.getPhoneNumber().trim();
        return IdempotencyService.hashPayload("stkpush", phone, amount);
    }
}
