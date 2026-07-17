package com.novastack.sms.controller;

import com.novastack.sms.domain.enums.TopupStatus;
import com.novastack.sms.domain.enums.UserRole;
import com.novastack.sms.domain.enums.WalletTransactionType;
import com.novastack.sms.dto.request.WalletTopupRequest;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.dto.response.StkPushResponse;
import com.novastack.sms.dto.response.WalletBalanceResponse;
import com.novastack.sms.dto.response.WalletTransactionResponse;
import com.novastack.sms.security.SecurityUtils;
import com.novastack.sms.security.UserPrincipal;
import com.novastack.sms.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @GetMapping("/balance")
    @Operation(summary = "Get wallet balance")
    public ApiResponse<WalletBalanceResponse> balance() {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(walletService.getBalance(orgId));
    }

    @PostMapping("/topup")
    @Operation(summary = "Top up wallet via M-Pesa Daraja STK Push to platform Paybill",
            description = "Triggers Lipa Na M-Pesa STK on the user's phone. Wallet is credited after successful payment callback.")
    public ApiResponse<StkPushResponse> topup(@Valid @RequestBody WalletTopupRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok("STK Push sent. Enter M-Pesa PIN on your phone.",
                walletService.initiateTopUp(orgId, request));
    }

    @GetMapping("/topup/{transactionId}")
    @Operation(summary = "Read top-up status from database (does not call Safaricom)")
    public ApiResponse<StkPushResponse> topupStatus(@PathVariable UUID transactionId) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(walletService.getTopUpStatus(orgId, transactionId));
    }

    @PostMapping("/topup/{transactionId}/check")
    @Operation(
            summary = "Check transaction: poll DB for Safaricom callback, and if still PENDING query Daraja then update DB",
            description = """
                    1) Reads wallet_transactions from MySQL.
                    2) If COMPLETED/FAILED — returns that status (callback already applied).
                    3) If PENDING — queries Safaricom STK status API and updates the database
                       (credits wallet on success, marks FAILED on cancel/failure).
                    Poll this every 3–5 seconds after initiating top-up.
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
