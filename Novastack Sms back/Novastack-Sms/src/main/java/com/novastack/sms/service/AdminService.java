package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.entity.User;
import com.novastack.sms.domain.entity.WalletTransaction;
import com.novastack.sms.domain.enums.BillingStatus;
import com.novastack.sms.domain.enums.MessageChannel;
import com.novastack.sms.domain.enums.OrganizationAccountType;
import com.novastack.sms.domain.enums.OrganizationBillingModel;
import com.novastack.sms.domain.enums.PaymentMethod;
import com.novastack.sms.domain.enums.OrganizationStatus;
import com.novastack.sms.domain.enums.SenderIdStatus;
import com.novastack.sms.domain.enums.TopupStatus;
import com.novastack.sms.domain.enums.UserRole;
import com.novastack.sms.domain.enums.WalletTransactionType;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.domain.repository.SenderIdRepository;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.domain.repository.UserRepository;
import com.novastack.sms.domain.repository.WalletRepository;
import com.novastack.sms.domain.repository.WalletTransactionRepository;
import com.novastack.sms.dto.request.AdminCreateOrganizationRequest;
import com.novastack.sms.dto.request.AdminCreditWalletRequest;
import com.novastack.sms.dto.request.UpdatePlatformBillingRequest;
import com.novastack.sms.dto.request.UpdatePlatformSmsSettingsRequest;
import com.novastack.sms.dto.response.AdminOrganizationResponse;
import com.novastack.sms.dto.response.PlatformBillingResponse;
import com.novastack.sms.dto.response.PlatformNotificationSettingsResponse;
import com.novastack.sms.dto.response.PlatformOverviewResponse;
import com.novastack.sms.dto.response.UserResponse;
import com.novastack.sms.dto.response.WalletTransactionResponse;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final SmsMessageRepository smsMessageRepository;
    private final SenderIdRepository senderIdRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletService walletService;
    private final BillingSettingsService billingSettingsService;
    private final SmsSettingsService smsSettingsService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    private final OrgNotificationService orgNotificationService;

    @Transactional(readOnly = true)
    public Page<AdminOrganizationResponse> listOrganizations(
            OrganizationStatus status,
            String search,
            Pageable pageable) {
        return organizationRepository.search(status, blankToNull(search), pageable)
                .map(this::toOrgResponse);
    }

    @Transactional(readOnly = true)
    public AdminOrganizationResponse getOrganization(UUID organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));
        return toOrgResponse(org);
    }

    @Transactional
    public AdminOrganizationResponse updateOrganizationStatus(UUID organizationId, OrganizationStatus status) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));
        org.setStatus(status);
        return toOrgResponse(organizationRepository.save(org));
    }

    @Transactional
    public AdminOrganizationResponse createOrganization(AdminCreateOrganizationRequest request) {
        String phone = PhoneNormalizer.normalize(request.getPhone());
        String email = request.getEmail().trim();
        if (organizationRepository.existsByEmailIgnoreCase(email) || userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException("Email already registered", HttpStatus.CONFLICT);
        }
        if (organizationRepository.existsByPhone(phone)) {
            throw new ApiException("Phone number already registered", HttpStatus.CONFLICT);
        }
        OrganizationAccountType accountType = request.getAccountType() != null
                ? request.getAccountType()
                : OrganizationAccountType.BUSINESS;
        OrganizationBillingModel billingModel = request.getBillingModel() != null
                ? request.getBillingModel()
                : OrganizationBillingModel.PREPAID;
        Organization organization = Organization.builder()
                .name(request.getName().trim())
                .email(email)
                .phone(phone)
                .apiKey(generateLegacyApiKey())
                .status(OrganizationStatus.ACTIVE)
                .accountType(accountType)
                .billingModel(billingModel)
                .smsCost(request.getSmsCost() != null ? request.getSmsCost() : billingSettingsService.customerPrice())
                .notificationsEnabled(true)
                .lowBalanceThreshold(smsSettingsService.lowBalanceThreshold())
                .build();
        organization = organizationRepository.save(organization);
        organization = walletService.ensureMpesaAccountRef(organization);
        walletService.createForOrganization(organization);

        if (request.getAdminFullName() != null && !request.getAdminFullName().isBlank()
                && request.getAdminPassword() != null && !request.getAdminPassword().isBlank()) {
            userRepository.save(User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(request.getAdminPassword()))
                    .fullName(request.getAdminFullName().trim())
                    .role(UserRole.ORGANIZATION_ADMIN)
                    .organization(organization)
                    .enabled(true)
                    .build());
        }

        orgNotificationService.notifyWelcome(organization);

        if (request.getInitialCredit() != null && request.getInitialCredit().compareTo(BigDecimal.ZERO) > 0) {
            walletService.adjust(
                    organization.getId(),
                    request.getInitialCredit(),
                    "ADJ-" + UUID.randomUUID(),
                    "Opening credit");
        }
        return toOrgResponse(organization);
    }

    @Transactional
    public AdminOrganizationResponse creditWallet(UUID organizationId, AdminCreditWalletRequest request) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ApiException("Organization not found", HttpStatus.NOT_FOUND);
        }
        String description = request.getDescription() == null || request.getDescription().isBlank()
                ? "Admin wallet credit"
                : request.getDescription().trim();
        walletService.adjust(organizationId, request.getAmount(), "ADJ-" + UUID.randomUUID(), description);
        return getOrganization(organizationId);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(
            UserRole role,
            UUID organizationId,
            String search,
            Pageable pageable) {
        return userRepository.search(role, organizationId, blankToNull(search), pageable)
                .map(user -> {
                    if (user.getOrganization() != null) {
                        user.getOrganization().getName();
                    }
                    return toUserResponse(user);
                });
    }

    @Transactional(readOnly = true)
    public PlatformOverviewResponse platformOverview() {
        BigDecimal threshold = smsSettingsService.lowBalanceThreshold();
        return PlatformOverviewResponse.builder()
                .organizations(organizationRepository.count())
                .users(userRepository.count())
                .superAdmins(userRepository.countByRole(UserRole.SUPER_ADMIN))
                .totalSmsSent(smsMessageRepository.count())
                .pendingSenderIds(senderIdRepository.countByStatus(SenderIdStatus.PENDING))
                .pendingTopups(walletTransactionRepository.countByTypeAndTopupStatus(
                        WalletTransactionType.TOPUP, TopupStatus.PENDING))
                .totalOrgWalletBalance(walletRepository.sumAllBalances())
                .currency(billingSettingsService.currency())
                .lowBalanceThreshold(threshold)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> listTopups(
            TopupStatus status,
            Pageable pageable) {
        boolean statusesEmpty = status == null;
        Collection<TopupStatus> statuses = statusesEmpty
                ? List.of(TopupStatus.PENDING)
                : List.of(status);

        return walletTransactionRepository
                .findPlatformFiltered(WalletTransactionType.TOPUP, statuses, statusesEmpty, pageable)
                .map(this::toTransactionResponse);
    }

    @Transactional(readOnly = true)
    public PlatformBillingResponse platformBilling() {
        BigDecimal customerPrice = billingSettingsService.customerPrice();
        BigDecimal providerCost = billingSettingsService.providerCostPerSms();
        BigDecimal grossMargin = customerPrice.subtract(providerCost).setScale(2, java.math.RoundingMode.HALF_UP);
        long totalSmsSent = smsMessageRepository.countByChannelAndBillingStatus(
                MessageChannel.SMS, BillingStatus.CHARGED);
        long totalSmsUnits = smsMessageRepository.sumUnitsByChannelAndBillingStatus(
                MessageChannel.SMS, BillingStatus.CHARGED);
        BigDecimal revenue = nullToZero(smsMessageRepository.sumCustomerRevenueByChannelAndBillingStatus(
                MessageChannel.SMS, BillingStatus.CHARGED));
        BigDecimal estimatedProviderCost = nullToZero(smsMessageRepository.sumProviderCostByChannelAndBillingStatus(
                MessageChannel.SMS, BillingStatus.CHARGED));
        BigDecimal totalMargin = nullToZero(smsMessageRepository.sumGrossMarginByChannelAndBillingStatus(
                MessageChannel.SMS, BillingStatus.CHARGED));
        return PlatformBillingResponse.builder()
                .provider(com.novastack.sms.provider.TalkSasaSmsProvider.PROVIDER_NAME)
                .defaultSenderId(appProperties.getSms().getTalksasa().resolvedDefaultSenderId())
                .customerSmsPrice(customerPrice)
                .providerCost(providerCost)
                .grossMargin(grossMargin)
                .currency(billingSettingsService.currency())
                .totalSmsSent(totalSmsSent)
                .totalSmsUnits(totalSmsUnits)
                .totalCustomerRevenue(revenue)
                .totalEstimatedProviderCost(estimatedProviderCost)
                .totalGrossMargin(totalMargin)
                .build();
    }

    @Transactional
    public PlatformBillingResponse updatePlatformBilling(UpdatePlatformBillingRequest request) {
        billingSettingsService.update(
                request.getCustomerSmsPrice(),
                request.getProviderCost(),
                request.getCurrency());
        return platformBilling();
    }

    @Transactional(readOnly = true)
    public PlatformNotificationSettingsResponse platformNotifications() {
        return toNotificationResponse(smsSettingsService.current());
    }

    @Transactional
    public PlatformNotificationSettingsResponse updatePlatformNotifications(
            UpdatePlatformSmsSettingsRequest request) {
        return toNotificationResponse(smsSettingsService.update(request));
    }

    private PlatformNotificationSettingsResponse toNotificationResponse(
            com.novastack.sms.domain.entity.PlatformSmsSettings settings) {
        return PlatformNotificationSettingsResponse.builder()
                .enabled(settings.isEnabled())
                .lowBalanceThreshold(smsSettingsService.lowBalanceThreshold())
                .portalUrl(smsSettingsService.portalUrl())
                .welcomeTemplate(smsSettingsService.welcomeTemplate())
                .topupTemplate(smsSettingsService.topupTemplate())
                .collectionTemplate(smsSettingsService.collectionTemplate())
                .lowBalanceTemplate(smsSettingsService.lowBalanceTemplate())
                .platformTopupTemplate(smsSettingsService.platformTopupTemplate())
                .providerLowTemplate(smsSettingsService.providerLowTemplate())
                .providerExposureTemplate(smsSettingsService.providerExposureTemplate())
                .talksasaLastRemaining(settings.getTalksasaLastRemaining())
                .talksasaLowAlerted(settings.isTalksasaLowAlerted())
                .talksasaExposureAlerted(settings.isTalksasaExposureAlerted())
                .collectionAccounts(smsSettingsService.collectionAccounts())
                .collectionNotifyPhones(smsSettingsService.collectionNotifyPhones())
                .build();
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        User user = userRepository.findByIdWithOrganization(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse setUserEnabled(UUID userId, boolean enabled) {
        User user = userRepository.findByIdWithOrganization(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        if (user.getRole() == UserRole.SUPER_ADMIN && !enabled) {
            throw new ApiException("Cannot disable SUPER_ADMIN", HttpStatus.BAD_REQUEST);
        }
        user.setEnabled(enabled);
        return toUserResponse(userRepository.save(user));
    }

    private AdminOrganizationResponse toOrgResponse(Organization org) {
        var wallet = walletRepository.findByOrganizationId(org.getId());
        return AdminOrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .email(org.getEmail())
                .phone(org.getPhone())
                .mpesaAccountRef(org.getMpesaAccountRef())
                .status(org.getStatus())
                .accountType(org.getAccountType())
                .billingModel(org.getBillingModel())
                .expiresAt(org.getExpiresAt())
                .smsCost(billingSettingsService.customerPrice())
                .walletBalance(wallet.map(w -> w.getBalance()).orElse(BigDecimal.ZERO))
                .currency(wallet.map(w -> w.getCurrency()).orElse("KES"))
                .userCount(userRepository.countByOrganizationId(org.getId()))
                .createdAt(org.getCreatedAt())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(resolveUserPhone(user))
                .fullName(user.getFullName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getName() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private static String resolveUserPhone(User user) {
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return user.getPhone().trim();
        }
        return user.getOrganization() != null ? user.getOrganization().getPhone() : null;
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction tx) {
        Organization org = tx.getOrganization();
        UUID organizationId = org != null ? org.getId() : null;
        if (org != null && !Hibernate.isInitialized(org)) {
            org = null;
        }
        String account = tx.getBillRef();
        if ((account == null || account.isBlank()) && org != null) {
            account = org.getMpesaAccountRef();
        }
        PaymentMethod method = tx.getPaymentMethod();
        if (method == null && tx.getCheckoutRequestId() != null && !tx.getCheckoutRequestId().isBlank()) {
            method = PaymentMethod.STK_PUSH;
        } else if (method == null) {
            method = PaymentMethod.PAYBILL;
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
                .paymentMethod(method)
                .paybill(appProperties.getMpesa().getShortcode())
                .accountNumber(account)
                .organizationName(org != null ? org.getName() : null)
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String generateLegacyApiKey() {
        return "nsk_" + UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
