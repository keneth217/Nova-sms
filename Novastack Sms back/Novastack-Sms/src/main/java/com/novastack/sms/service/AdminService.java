package com.novastack.sms.service;

import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.entity.User;
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
import com.novastack.sms.dto.response.AdminOrganizationResponse;
import com.novastack.sms.dto.response.UserResponse;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
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
    public Map<String, Long> platformOverview() {
        Map<String, Long> overview = new HashMap<>();
        overview.put("organizations", organizationRepository.count());
        overview.put("users", userRepository.count());
        overview.put("superAdmins", userRepository.countByRole(UserRole.SUPER_ADMIN));
        overview.put("totalSmsSent", smsMessageRepository.count());
        overview.put("pendingSenderIds", senderIdRepository.countByStatus(SenderIdStatus.PENDING));
        overview.put(
                "pendingTopups",
                walletTransactionRepository.countByTypeAndTopupStatus(
                        WalletTransactionType.TOPUP, TopupStatus.PENDING));
        return overview;
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
                .apiKey(org.getApiKey())
                .mpesaAccountRef(org.getMpesaAccountRef())
                .status(org.getStatus())
                .accountType(org.getAccountType())
                .expiresAt(org.getExpiresAt())
                .smsCost(org.getSmsCost())
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
                .fullName(user.getFullName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getName() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
