package com.novastack.sms.security;

import com.novastack.sms.domain.entity.ApiClient;
import com.novastack.sms.domain.entity.User;
import com.novastack.sms.domain.enums.ApiPermission;
import com.novastack.sms.domain.enums.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final String fullName;
    private final UserRole role;
    private final UUID organizationId;
    private final boolean enabled;
    private final long tokenVersion;
    private final String apiKeyPrefix;
    private final boolean apiKeyAuth;
    private final UUID apiClientId;
    private final Set<ApiPermission> apiPermissions;

    private UserPrincipal(
            UUID id,
            String email,
            String password,
            String fullName,
            UserRole role,
            UUID organizationId,
            boolean enabled,
            long tokenVersion,
            String apiKeyPrefix,
            boolean apiKeyAuth,
            UUID apiClientId,
            Set<ApiPermission> apiPermissions) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.organizationId = organizationId;
        this.enabled = enabled;
        this.tokenVersion = tokenVersion;
        this.apiKeyPrefix = apiKeyPrefix;
        this.apiKeyAuth = apiKeyAuth;
        this.apiClientId = apiClientId;
        this.apiPermissions = apiPermissions == null ? Set.of() : Set.copyOf(apiPermissions);
    }

    public static UserPrincipal fromUser(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getFullName(),
                user.getRole(),
                user.getOrganization() != null ? user.getOrganization().getId() : null,
                user.isEnabled(),
                user.getTokenVersion(),
                null,
                false,
                null,
                Set.of()
        );
    }

    public static UserPrincipal fromLegacyApiKey(UUID organizationId, String orgName) {
        return new UserPrincipal(
                null,
                orgName + "@api",
                "",
                orgName,
                UserRole.ORGANIZATION_ADMIN,
                organizationId,
                true,
                0L,
                null,
                true,
                null,
                Set.of()
        );
    }

    public static UserPrincipal fromApiKey(UUID organizationId, String apiKey, String orgName) {
        return fromLegacyApiKey(organizationId, orgName);
    }

    public static UserPrincipal fromApiClient(ApiClient client) {
        UUID orgId = client.getOrganization() != null ? client.getOrganization().getId() : null;
        String orgName = client.getOrganization() != null ? client.getOrganization().getName() : client.getName();
        Set<ApiPermission> permissions = client.getPermissions() == null || client.getPermissions().isEmpty()
                ? EnumSet.noneOf(ApiPermission.class)
                : EnumSet.copyOf(client.getPermissions());
        return new UserPrincipal(
                null,
                client.getClientCode() + "@api",
                "",
                client.getName(),
                UserRole.ORGANIZATION_ADMIN,
                orgId,
                true,
                0L,
                client.getApiKeyPrefix(),
                true,
                client.getId(),
                permissions
        );
    }

    public boolean isScopedApiClient() {
        return apiClientId != null;
    }

    public boolean hasPermission(ApiPermission permission) {
        if (!isScopedApiClient()) {
            return true;
        }
        if (apiPermissions.contains(permission)) {
            return true;
        }
        if (permission == ApiPermission.MPESA_STK_PUSH && apiPermissions.contains(ApiPermission.WALLET_TOPUP)) {
            return true;
        }
        if (permission == ApiPermission.MPESA_STATUS
                && (apiPermissions.contains(ApiPermission.WALLET_TOPUP)
                || apiPermissions.contains(ApiPermission.MPESA_STK_PUSH))) {
            return true;
        }
        if (permission == ApiPermission.MPESA_C2B
                && (apiPermissions.contains(ApiPermission.WALLET_READ)
                || apiPermissions.contains(ApiPermission.WALLET_TOPUP))) {
            return true;
        }
        return false;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
