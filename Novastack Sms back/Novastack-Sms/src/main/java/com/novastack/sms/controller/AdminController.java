package com.novastack.sms.controller;

import com.novastack.sms.domain.entity.SenderId;
import com.novastack.sms.domain.enums.OrganizationStatus;
import com.novastack.sms.domain.enums.SenderIdStatus;
import com.novastack.sms.domain.enums.TopupStatus;
import com.novastack.sms.domain.enums.UserRole;
import com.novastack.sms.dto.request.AdminCreateOrganizationRequest;
import com.novastack.sms.dto.request.AdminCreditWalletRequest;
import com.novastack.sms.dto.request.CreditMpesaReceiptRequest;
import com.novastack.sms.dto.request.VerifyMpesaReceiptRequest;
import com.novastack.sms.dto.request.CreateApiClientRequest;
import com.novastack.sms.dto.request.SendSmsRequest;
import com.novastack.sms.dto.request.UpdateAnnouncementRequest;
import com.novastack.sms.dto.request.UpdateApiClientRequest;
import com.novastack.sms.dto.request.UpdatePlatformBillingRequest;
import com.novastack.sms.dto.request.UpdatePlatformSmsSettingsRequest;
import com.novastack.sms.dto.response.AdminOrganizationResponse;
import com.novastack.sms.dto.response.AnnouncementResponse;
import com.novastack.sms.dto.response.ApiClientCreatedResponse;
import com.novastack.sms.dto.response.ApiClientResponse;
import com.novastack.sms.dto.response.ApiClientUsageResponse;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.dto.response.DeveloperConfigResponse;
import com.novastack.sms.dto.response.MpesaReceiptLookupResponse;
import com.novastack.sms.dto.response.PaybillCollectionDashboardResponse;
import com.novastack.sms.dto.response.PlatformBillingResponse;
import com.novastack.sms.dto.response.PlatformNotificationSettingsResponse;
import com.novastack.sms.dto.response.PlatformOverviewResponse;
import com.novastack.sms.dto.response.SmsMessageResponse;
import com.novastack.sms.dto.response.StkPushResponse;
import com.novastack.sms.dto.response.TalkSasaAccountResponse;
import com.novastack.sms.dto.response.TalkSasaSmsListResponse;
import com.novastack.sms.dto.response.TalkSasaSmsViewResponse;
import com.novastack.sms.dto.response.UserResponse;
import com.novastack.sms.dto.response.WalletTransactionResponse;
import com.novastack.sms.service.AdminService;
import com.novastack.sms.service.AnnouncementService;
import com.novastack.sms.service.ApiClientService;
import com.novastack.sms.service.DeveloperPortalService;
import com.novastack.sms.service.PaybillCollectionService;
import com.novastack.sms.service.SenderIdService;
import com.novastack.sms.service.SmsService;
import com.novastack.sms.service.TalkSasaInboxService;
import com.novastack.sms.service.WalletService;
import com.novastack.sms.provider.TalkSasaProfileClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Super Admin")
public class AdminController {

    private final AdminService adminService;
    private final ApiClientService apiClientService;
    private final SmsService smsService;
    private final SenderIdService senderIdService;
    private final TalkSasaProfileClient talkSasaProfileClient;
    private final TalkSasaInboxService talkSasaInboxService;
    private final DeveloperPortalService developerPortalService;
    private final WalletService walletService;
    private final PaybillCollectionService paybillCollectionService;
    private final AnnouncementService announcementService;

    @GetMapping("/organizations")
    @Operation(summary = "List all registered organizations")
    public ApiResponse<Page<AdminOrganizationResponse>> listOrganizations(
            @RequestParam(required = false) OrganizationStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(adminService.listOrganizations(status, search, pageable));
    }

    @PostMapping("/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an organization (internal apps such as Mwalimu / Chamaplus)")
    public ApiResponse<AdminOrganizationResponse> createOrganization(
            @Valid @RequestBody AdminCreateOrganizationRequest request) {
        return ApiResponse.ok(adminService.createOrganization(request));
    }

    @PostMapping("/organizations/{organizationId}/wallet/credit")
    @Operation(summary = "Credit an organization wallet (monthly allocation or internal funding)")
    public ApiResponse<AdminOrganizationResponse> creditWallet(
            @PathVariable UUID organizationId,
            @Valid @RequestBody AdminCreditWalletRequest request) {
        return ApiResponse.ok(adminService.creditWallet(organizationId, request));
    }

    @GetMapping("/organizations/{organizationId}")
    @Operation(summary = "Get organization details")
    public ApiResponse<AdminOrganizationResponse> getOrganization(@PathVariable UUID organizationId) {
        return ApiResponse.ok(adminService.getOrganization(organizationId));
    }

    @PatchMapping("/organizations/{organizationId}/status")
    @Operation(summary = "Update organization status (ACTIVE / SUSPENDED / PENDING)")
    public ApiResponse<AdminOrganizationResponse> updateOrganizationStatus(
            @PathVariable UUID organizationId,
            @RequestParam OrganizationStatus status) {
        return ApiResponse.ok(adminService.updateOrganizationStatus(organizationId, status));
    }

    @GetMapping("/users")
    @Operation(summary = "List all registered users/people across organizations")
    public ApiResponse<Page<UserResponse>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(adminService.listUsers(role, organizationId, search, pageable));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user details")
    public ApiResponse<UserResponse> getUser(@PathVariable UUID userId) {
        return ApiResponse.ok(adminService.getUser(userId));
    }

    @PatchMapping("/users/{userId}/enabled")
    @Operation(summary = "Enable or disable a user")
    public ApiResponse<UserResponse> setUserEnabled(
            @PathVariable UUID userId,
            @RequestParam boolean enabled) {
        return ApiResponse.ok(adminService.setUserEnabled(userId, enabled));
    }

    @GetMapping("/overview")
    @Operation(summary = "Platform overview counts")
    public ApiResponse<PlatformOverviewResponse> overview() {
        return ApiResponse.ok(adminService.platformOverview());
    }

    @GetMapping("/talksasa")
    @Operation(summary = "TalkSasa platform profile and SMS unit balance")
    public ApiResponse<TalkSasaAccountResponse> talksasaAccount() {
        return ApiResponse.ok(talkSasaProfileClient.getAccount());
    }

    @GetMapping("/talksasa/sms")
    @Operation(summary = "Live TalkSasa message list (GET /sms). Super Admin only; shared provider inbox")
    public ApiResponse<TalkSasaSmsListResponse> talksasaSms(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ApiResponse.ok(talkSasaInboxService.list(page, size));
    }

    @GetMapping("/talksasa/sms/{uid}")
    @Operation(summary = "Live TalkSasa message (GET /sms/{uid}). Super Admin only")
    public ApiResponse<TalkSasaSmsViewResponse> talksasaSmsOne(@PathVariable String uid) {
        return ApiResponse.ok(talkSasaInboxService.get(uid));
    }

    @GetMapping("/billing")
    @Operation(summary = "Platform SMS billing settings and Super Admin revenue report")
    public ApiResponse<PlatformBillingResponse> billing() {
        return ApiResponse.ok(adminService.platformBilling());
    }

    @PutMapping("/billing")
    @Operation(summary = "Update platform customer SMS price and internal provider cost")
    public ApiResponse<PlatformBillingResponse> updateBilling(
            @RequestBody UpdatePlatformBillingRequest request) {
        return ApiResponse.ok(adminService.updatePlatformBilling(request));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Platform SMS notification settings and message templates")
    public ApiResponse<PlatformNotificationSettingsResponse> notifications() {
        return ApiResponse.ok(adminService.platformNotifications());
    }

    @PutMapping("/notifications")
    @Operation(summary = "Update platform SMS notification templates and defaults")
    public ApiResponse<PlatformNotificationSettingsResponse> updateNotifications(
            @Valid @RequestBody UpdatePlatformSmsSettingsRequest request) {
        return ApiResponse.ok(adminService.updatePlatformNotifications(request));
    }

    @GetMapping("/announcement")
    @Operation(summary = "Dashboard announcement banner")
    public ApiResponse<AnnouncementResponse> announcement() {
        return ApiResponse.ok(announcementService.current());
    }

    @PutMapping("/announcement")
    @Operation(summary = "Update dashboard announcement banner")
    public ApiResponse<AnnouncementResponse> updateAnnouncement(
            @Valid @RequestBody UpdateAnnouncementRequest request) {
        return ApiResponse.ok("Announcement saved", announcementService.update(request));
    }

    @GetMapping("/mpesa/c2b/urls")
    @Operation(summary = "C2B confirmation and validation URLs Nova will register with Daraja")
    public ApiResponse<Map<String, String>> c2bUrls() {
        return ApiResponse.ok(walletService.c2bCallbackUrls());
    }

    @PostMapping("/mpesa/c2b/register")
    @Operation(summary = "Register C2B v2 confirmation and validation URLs for the platform Paybill")
    public ApiResponse<Map<String, String>> registerC2b() {
        return ApiResponse.ok("C2B v2 URLs registered", walletService.registerC2bV2Urls());
    }

    @GetMapping("/collections")
    @Operation(summary = "Paybill collection stats for non-wallet accounts (SHEILA, KENETH, …)")
    public ApiResponse<PaybillCollectionDashboardResponse> collections(
            @RequestParam(required = false) String billRef,
            @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.ok(paybillCollectionService.dashboard(billRef, pageable));
    }

    @GetMapping("/topups")
    @Operation(summary = "List wallet top-ups across all organizations")
    public ApiResponse<Page<WalletTransactionResponse>> listTopups(
            @RequestParam(required = false) TopupStatus status,
            @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.ok(adminService.listTopups(status, pageable));
    }

    @PostMapping("/topups/verify-receipt")
    @Operation(summary = "Verify a receipt; if Nova stored the C2B BillRefNumber, credit that org wallet automatically")
    public ApiResponse<MpesaReceiptLookupResponse> verifyReceipt(@Valid @RequestBody VerifyMpesaReceiptRequest request) {
        return ApiResponse.ok(walletService.verifyReceiptPlatform(request.getMpesaReceipt()));
    }

    @PostMapping("/topups/credit-receipt")
    @Operation(summary = "Manual recovery when C2B metadata is missing: resolve org from Paybill account, never from organizationId")
    public ApiResponse<MpesaReceiptLookupResponse> creditReceipt(@Valid @RequestBody CreditMpesaReceiptRequest request) {
        return ApiResponse.ok(walletService.creditByMpesaReceipt(
                request.getAccountNumber(), request.getMpesaReceipt(), request.getAmount()));
    }

    @PostMapping("/topups/{transactionId}/check")
    @Operation(summary = "Re-query Safaricom for a top-up and credit the wallet if the payment succeeded")
    public ApiResponse<StkPushResponse> checkTopup(@PathVariable UUID transactionId) {
        return ApiResponse.ok(walletService.checkTopUpTransaction(transactionId));
    }

    @GetMapping("/sms")
    @Operation(summary = "List SMS messages across all organizations")
    public ApiResponse<Page<SmsMessageResponse>> listSms(@PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.ok(smsService.platformHistory(pageable));
    }

    @GetMapping("/sender-ids")
    @Operation(summary = "List sender IDs across all organizations")
    public ApiResponse<List<SenderId>> listSenderIds(
            @RequestParam(required = false) SenderIdStatus status) {
        return ApiResponse.ok(senderIdService.listAll(status));
    }

    @GetMapping("/api-clients")
    @Operation(summary = "List API clients across organizations")
    public ApiResponse<Page<ApiClientResponse>> listApiClients(
            @RequestParam(required = false) UUID organizationId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(apiClientService.listAll(organizationId, pageable));
    }

    @PostMapping("/api-clients")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an API client for an organization. Plaintext key returned once.")
    public ApiResponse<ApiClientCreatedResponse> createApiClient(
            @Valid @RequestBody CreateApiClientRequest request) {
        return ApiResponse.ok("Store this API key now. It will not be shown again.",
                apiClientService.create(request.getOrganizationId(), request));
    }

    @PatchMapping("/api-clients/{clientId}")
    public ApiResponse<ApiClientResponse> updateApiClient(
            @PathVariable UUID clientId,
            @Valid @RequestBody UpdateApiClientRequest request) {
        return ApiResponse.ok(apiClientService.update(null, clientId, request));
    }

    @PostMapping("/api-clients/{clientId}/rotate")
    public ApiResponse<ApiClientCreatedResponse> rotateApiClient(@PathVariable UUID clientId) {
        return ApiResponse.ok("Store this API key now. It will not be shown again.",
                apiClientService.rotate(null, clientId));
    }

    @PatchMapping("/api-clients/{clientId}/enabled")
    public ApiResponse<ApiClientResponse> setApiClientEnabled(
            @PathVariable UUID clientId,
            @RequestParam boolean enabled) {
        return ApiResponse.ok(apiClientService.setEnabled(null, clientId, enabled));
    }

    @PostMapping("/api-clients/{clientId}/revoke")
    public ApiResponse<ApiClientResponse> revokeApiClient(@PathVariable UUID clientId) {
        return ApiResponse.ok(apiClientService.revoke(null, clientId));
    }

    @GetMapping("/developer/config")
    @Operation(summary = "Public API origin and OpenAPI paths for the developer portal")
    public ApiResponse<DeveloperConfigResponse> developerConfig() {
        return ApiResponse.ok(developerPortalService.publicConfig());
    }

    @GetMapping("/api-clients/{clientId}/usage")
    @Operation(summary = "SMS usage attributed to an API client")
    public ApiResponse<ApiClientUsageResponse> apiClientUsage(@PathVariable UUID clientId) {
        return ApiResponse.ok(developerPortalService.usage(clientId));
    }

    @PostMapping("/api-clients/{clientId}/test-send")
    @Operation(summary = "Send a test SMS as an API client without exposing the API key")
    public ApiResponse<SmsMessageResponse> testSend(
            @PathVariable UUID clientId,
            @Valid @RequestBody SendSmsRequest request) {
        return ApiResponse.ok("SMS queued", developerPortalService.testSend(clientId, request));
    }

    @GetMapping("/sms/{id}")
    @Operation(summary = "Get any organization SMS by Nova id or TalkSasa uid")
    public ApiResponse<SmsMessageResponse> getSms(@PathVariable String id) {
        return ApiResponse.ok(smsService.getByIdOrProviderUid(id));
    }

    @GetMapping("/sms/{id}/status")
    @Operation(summary = "Refresh delivery status for any organization SMS")
    public ApiResponse<SmsMessageResponse> refreshSmsStatus(@PathVariable String id) {
        return ApiResponse.ok(smsService.refreshStatusById(id));
    }
}
