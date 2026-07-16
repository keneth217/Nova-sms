package com.novastack.sms.security;

import com.novastack.sms.domain.enums.UserRole;
import com.novastack.sms.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException("Unauthenticated", HttpStatus.UNAUTHORIZED);
        }
        return principal;
    }

    public static UUID requireOrganizationId() {
        UserPrincipal principal = currentUser();
        if (principal.getRole() == UserRole.SUPER_ADMIN && principal.getOrganizationId() == null) {
            throw new ApiException("Organization context required", HttpStatus.BAD_REQUEST);
        }
        if (principal.getOrganizationId() == null) {
            throw new ApiException("Organization not associated with user", HttpStatus.FORBIDDEN);
        }
        return principal.getOrganizationId();
    }

    /**
     * Organization from auth context, or explicit organizationId when caller is SUPER_ADMIN.
     */
    public static UUID resolveOrganizationId(UUID requestedOrganizationId) {
        UserPrincipal principal = currentUser();
        if (requestedOrganizationId != null) {
            if (principal.getRole() != UserRole.SUPER_ADMIN
                    && !requestedOrganizationId.equals(principal.getOrganizationId())) {
                throw new ApiException("Cannot view another organization's transactions", HttpStatus.FORBIDDEN);
            }
            return requestedOrganizationId;
        }
        return requireOrganizationId();
    }
}
