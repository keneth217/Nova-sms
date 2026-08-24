package com.novastack.sms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.domain.enums.UserRole;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.service.OrganizationAccessService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Blocks EVENT organizations whose 1-week window has ended from using org APIs.
 */
@Component
@RequiredArgsConstructor
public class OrganizationExpiryFilter extends OncePerRequestFilter {

    private final OrganizationAccessService organizationAccessService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/api/v1/auth")
                || path.contains("/api/v1/organizations/register")
                || path.contains("/api/v1/dlr")
                || path.contains("/api/v1/mpesa/stk/callback")
                || path.contains("/api/v1/mpesa/c2b/confirmation")
                || path.contains("/api/v1/mpesa/c2b/validation")
                || path.contains("/api/v1/payments")
                || path.contains("/api/v1/data-bundles")
                || path.contains("/api/v1/admin")
                || path.contains("/actuator")
                || path.contains("/v3/api-docs")
                || path.contains("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            if (principal.getRole() != UserRole.SUPER_ADMIN && principal.getOrganizationId() != null) {
                try {
                    organizationAccessService.ensureUsable(principal.getOrganizationId());
                } catch (ApiException ex) {
                    response.setStatus(ex.getStatus().value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(
                            response.getOutputStream(),
                            ApiResponse.fail(ex.getMessage()));
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
