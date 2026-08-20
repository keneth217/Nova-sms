package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.entity.SenderId;
import com.novastack.sms.domain.entity.User;
import com.novastack.sms.domain.entity.Wallet;
import com.novastack.sms.domain.enums.OrganizationAccountType;
import com.novastack.sms.domain.enums.OrganizationStatus;
import com.novastack.sms.domain.enums.SenderIdStatus;
import com.novastack.sms.domain.enums.UserRole;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.domain.repository.SenderIdRepository;
import com.novastack.sms.domain.repository.UserRepository;
import com.novastack.sms.dto.request.ChangePasswordRequest;
import com.novastack.sms.dto.request.LoginRequest;
import com.novastack.sms.dto.request.OrganizationRegisterRequest;
import com.novastack.sms.dto.request.OrganizationSettingsRequest;
import com.novastack.sms.dto.response.AuthResponse;
import com.novastack.sms.dto.response.OrganizationResponse;
import com.novastack.sms.dto.response.UserResponse;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.security.JwtService;
import com.novastack.sms.security.SecurityUtils;
import com.novastack.sms.security.UserPrincipal;
import com.novastack.sms.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final SenderIdRepository senderIdRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;
    private final OrganizationAccessService organizationAccessService;
    private final WalletService walletService;
    private final OrgNotificationService orgNotificationService;
    private final SmsSettingsService smsSettingsService;

    @Transactional
    public OrganizationResponse register(OrganizationRegisterRequest request) {
        if (organizationRepository.existsByEmailIgnoreCase(request.getEmail())
                || userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ApiException("Email already registered", HttpStatus.CONFLICT);
        }

        String phone = PhoneNormalizer.normalize(request.getPhone());
        if (organizationRepository.existsByPhone(phone)) {
            throw new ApiException("Phone number already registered", HttpStatus.CONFLICT);
        }

        OrganizationAccountType accountType = request.getAccountType() != null
                ? request.getAccountType()
                : OrganizationAccountType.BUSINESS;

        Instant expiresAt = null;
        Integer activeDays = null;
        if (accountType == OrganizationAccountType.EVENT) {
            activeDays = appProperties.getOrganization().getEventActiveDays();
            expiresAt = Instant.now().plus(activeDays, ChronoUnit.DAYS);
        }

        Organization organization = Organization.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(phone)
                .apiKey(generateApiKey())
                .status(OrganizationStatus.ACTIVE)
                .accountType(accountType)
                .expiresAt(expiresAt)
                .smsCost(appProperties.getSms().getDefaultCost())
                .notificationsEnabled(true)
                .lowBalanceThreshold(smsSettingsService.lowBalanceThreshold())
                .build();
        organization = organizationRepository.save(organization);
        organization = walletService.ensureMpesaAccountRef(organization);

        // Prepaid wallet for M-Pesa top-ups and SMS sending
        var wallet = walletService.createForOrganization(organization);

        Instant acceptedAt = Instant.now();
        User admin = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getAdminFullName())
                .role(UserRole.ORGANIZATION_ADMIN)
                .organization(organization)
                .enabled(true)
                .termsAccepted(true)
                .termsAcceptedAt(acceptedAt)
                .privacyAcceptedAt(acceptedAt)
                .build();
        userRepository.save(admin);

        orgNotificationService.notifyWelcome(organization);

        return toOrgResponse(organization, activeDays, wallet);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getEmail() == null ? "" : request.getEmail().trim();
        if (identifier.isBlank()) {
            throw new ApiException("Email or phone is required", HttpStatus.BAD_REQUEST);
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, request.getPassword()));

        User user = resolveUser(identifier)
                .orElseThrow(() -> new ApiException("Invalid email/phone or password", HttpStatus.UNAUTHORIZED));

        Organization organization = user.getOrganization();
        if (organization != null) {
            organizationAccessService.ensureUsable(organization);
        }

        UserPrincipal principal = UserPrincipal.fromUser(user);
        String token = jwtService.generateToken(principal);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .phone(resolveUserPhone(user))
                .fullName(user.getFullName())
                .role(user.getRole())
                .organizationId(organization != null ? organization.getId() : null)
                .organizationName(organization != null ? organization.getName() : null)
                .accountType(organization != null ? organization.getAccountType() : null)
                .expiresAt(organization != null ? organization.getExpiresAt() : null)
                .build();
    }

    @Transactional
    public OrganizationResponse getCurrentOrganization() {
        UUID organizationId = SecurityUtils.requireOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));
        organization = walletService.ensureMpesaAccountRef(organization);
        Wallet wallet = walletService.ensureWallet(organizationId);
        Integer activeDays = null;
        if (organization.getAccountType() == OrganizationAccountType.EVENT
                && organization.getExpiresAt() != null) {
            long days = ChronoUnit.DAYS.between(Instant.now(), organization.getExpiresAt());
            activeDays = (int) Math.max(0, days);
        }
        return toOrgResponse(organization, activeDays, wallet);
    }

    @Transactional
    public OrganizationResponse updateSettings(OrganizationSettingsRequest request) {
        UUID organizationId = SecurityUtils.requireOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));
        applyOrganizationProfile(organization, request);
        organization.setNotificationsEnabled(Boolean.TRUE.equals(request.getNotificationsEnabled()));
        organization.setLowBalanceThreshold(
                request.getLowBalanceThreshold().setScale(2, java.math.RoundingMode.HALF_UP));
        organization = organizationRepository.save(organization);
        Wallet wallet = walletService.ensureWallet(organizationId);
        Integer activeDays = null;
        if (organization.getAccountType() == OrganizationAccountType.EVENT
                && organization.getExpiresAt() != null) {
            long days = ChronoUnit.DAYS.between(Instant.now(), organization.getExpiresAt());
            activeDays = (int) Math.max(0, days);
        }
        return toOrgResponse(organization, activeDays, wallet);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentProfile() {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.getId() == null || principal.isApiKeyAuth()) {
            throw new ApiException("User profile requires JWT authentication", HttpStatus.FORBIDDEN);
        }
        User user = userRepository.findByIdWithOrganization(principal.getId())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return toUserResponse(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.getId() == null || principal.isApiKeyAuth()) {
            throw new ApiException("Password change requires JWT authentication", HttpStatus.FORBIDDEN);
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ApiException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ApiException(
                    "New password must be different from the current password",
                    HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    private Optional<User> resolveUser(String identifier) {
        return userRepository.findByEmailWithOrganization(identifier)
                .or(() -> {
                    if (PhoneNormalizer.looksLikePhone(identifier)) {
                        String normalized = PhoneNormalizer.normalize(identifier);
                        return userRepository.findOrgAdminByOrganizationPhone(normalized)
                                .or(() -> userRepository.findOrgAdminByOrganizationPhone(identifier));
                    }
                    return Optional.empty();
                });
    }

    @Transactional
    public void ensurePlatformSender() {
        senderIdRepository.findFirstByPlatformDefaultTrueAndStatus(SenderIdStatus.APPROVED)
                .orElseGet(() -> senderIdRepository.save(SenderId.builder()
                        .senderName(appProperties.getSms().getPlatformSenderId())
                        .status(SenderIdStatus.APPROVED)
                        .platformDefault(true)
                        .build()));
    }

    @Transactional
    public void backfillMpesaAccountRefs() {
        organizationRepository.findAll().forEach(walletService::ensureMpesaAccountRef);
    }

    @Transactional
    public void ensureSuperAdmin() {
        var cfg = appProperties.getSuperAdmin();
        String rawPhone = blankToNull(cfg.getPhone());
        final String phone = rawPhone == null ? null : PhoneNormalizer.normalizeKenyanMobile(rawPhone);
        userRepository.findByEmailIgnoreCase(cfg.getEmail()).ifPresentOrElse(user -> {
            if (phone != null && !phone.equals(user.getPhone())) {
                user.setPhone(phone);
                userRepository.save(user);
            }
        }, () -> userRepository.save(User.builder()
                .email(cfg.getEmail())
                .password(passwordEncoder.encode(cfg.getPassword()))
                .fullName(cfg.getFullName())
                .phone(phone)
                .role(UserRole.SUPER_ADMIN)
                .organization(null)
                .enabled(true)
                .build()));
    }

    private String generateApiKey() {
        return "nsk_" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private OrganizationResponse toOrgResponse(
            Organization organization,
            Integer activeDays,
            Wallet wallet) {
        return OrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .email(organization.getEmail())
                .phone(organization.getPhone())
                .apiKey(organization.getApiKey())
                .mpesaAccountRef(organization.getMpesaAccountRef())
                .status(organization.getStatus())
                .accountType(organization.getAccountType())
                .expiresAt(organization.getExpiresAt())
                .activeDays(activeDays)
                .createdAt(organization.getCreatedAt())
                .walletId(wallet != null ? wallet.getId() : null)
                .walletBalance(wallet != null ? wallet.getBalance() : null)
                .walletCurrency(wallet != null ? wallet.getCurrency() : "KES")
                .notificationsEnabled(organization.isNotificationsEnabled())
                .lowBalanceThreshold(organization.getLowBalanceThreshold() != null
                        ? organization.getLowBalanceThreshold()
                        : smsSettingsService.lowBalanceThreshold())
                .platformNotificationsEnabled(smsSettingsService.isEnabled())
                .platformLowBalanceThreshold(smsSettingsService.lowBalanceThreshold())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        Organization organization = user.getOrganization();
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(resolveUserPhone(user))
                .fullName(user.getFullName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .organizationId(organization != null ? organization.getId() : null)
                .organizationName(organization != null ? organization.getName() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private void applyOrganizationProfile(Organization organization, OrganizationSettingsRequest request) {
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isBlank()) {
                throw new ApiException("Organization name is required", HttpStatus.BAD_REQUEST);
            }
            organization.setName(name);
        }
        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (email.isBlank()) {
                throw new ApiException("Organization email is required", HttpStatus.BAD_REQUEST);
            }
            applyOrganizationEmail(organization, email);
        }
        if (request.getPhone() != null) {
            String phone = PhoneNormalizer.normalizeKenyanMobile(request.getPhone());
            if (!phone.equals(organization.getPhone())
                    && organizationRepository.existsByPhoneAndIdNot(phone, organization.getId())) {
                throw new ApiException("Phone number already registered", HttpStatus.CONFLICT);
            }
            organization.setPhone(phone);
        }
    }

    private void applyOrganizationEmail(Organization organization, String email) {
        String current = organization.getEmail();
        if (current != null && current.equalsIgnoreCase(email)) {
            organization.setEmail(email);
            return;
        }
        if (organizationRepository.existsByEmailIgnoreCaseAndIdNot(email, organization.getId())) {
            throw new ApiException("Email already registered", HttpStatus.CONFLICT);
        }
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            UUID existingOrgId = existing.getOrganization() != null ? existing.getOrganization().getId() : null;
            if (!organization.getId().equals(existingOrgId)) {
                throw new ApiException("Email already registered", HttpStatus.CONFLICT);
            }
        });
        String previous = current;
        organization.setEmail(email);
        if (previous == null || previous.isBlank()) {
            return;
        }
        for (User member : userRepository.findByOrganizationId(organization.getId())) {
            if (member.getEmail() != null && member.getEmail().equalsIgnoreCase(previous)) {
                member.setEmail(email);
                userRepository.save(member);
            }
        }
    }

    private static String resolveUserPhone(User user) {
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return user.getPhone().trim();
        }
        Organization organization = user.getOrganization();
        return organization != null ? organization.getPhone() : null;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
