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

    @Transactional
    public OrganizationResponse register(OrganizationRegisterRequest request) {
        if (organizationRepository.existsByEmail(request.getEmail()) || userRepository.existsByEmail(request.getEmail())) {
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
                .build();
        organization = organizationRepository.save(organization);
        organization.setMpesaAccountRef(buildMpesaAccountRef(organization.getId()));
        organization = organizationRepository.save(organization);

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
                .fullName(user.getFullName())
                .role(user.getRole())
                .organizationId(organization != null ? organization.getId() : null)
                .organizationName(organization != null ? organization.getName() : null)
                .accountType(organization != null ? organization.getAccountType() : null)
                .expiresAt(organization != null ? organization.getExpiresAt() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getCurrentOrganization() {
        UUID organizationId = SecurityUtils.requireOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));
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
        organizationRepository.findAll().stream()
                .filter(org -> org.getMpesaAccountRef() == null || org.getMpesaAccountRef().isBlank())
                .forEach(org -> {
                    org.setMpesaAccountRef(buildMpesaAccountRef(org.getId()));
                    organizationRepository.save(org);
                });
    }

    @Transactional
    public void ensureSuperAdmin() {
        var cfg = appProperties.getSuperAdmin();
        if (userRepository.existsByEmail(cfg.getEmail())) {
            return;
        }
        userRepository.save(User.builder()
                .email(cfg.getEmail())
                .password(passwordEncoder.encode(cfg.getPassword()))
                .fullName(cfg.getFullName())
                .role(UserRole.SUPER_ADMIN)
                .organization(null)
                .enabled(true)
                .build());
    }

    private String generateApiKey() {
        return "nsk_" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String buildMpesaAccountRef(UUID organizationId) {
        String prefix = appProperties.getMpesa().getAccountReferencePrefix();
        String compact = organizationId.toString().replace("-", "");
        return (prefix + compact.substring(0, Math.min(8, compact.length()))).toUpperCase();
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
                .build();
    }

    private UserResponse toUserResponse(User user) {
        Organization organization = user.getOrganization();
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .organizationId(organization != null ? organization.getId() : null)
                .organizationName(organization != null ? organization.getName() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
