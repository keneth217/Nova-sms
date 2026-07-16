package com.novastack.sms.security;

import com.novastack.sms.domain.entity.User;
import com.novastack.sms.domain.enums.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
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
    private final String apiKey;
    private final boolean apiKeyAuth;

    private UserPrincipal(UUID id, String email, String password, String fullName, UserRole role,
                          UUID organizationId, boolean enabled, String apiKey, boolean apiKeyAuth) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.organizationId = organizationId;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.apiKeyAuth = apiKeyAuth;
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
                null,
                false
        );
    }

    public static UserPrincipal fromApiKey(UUID organizationId, String apiKey, String orgName) {
        return new UserPrincipal(
                null,
                orgName + "@api",
                "",
                orgName,
                UserRole.ORGANIZATION_ADMIN,
                organizationId,
                true,
                apiKey,
                true
        );
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
