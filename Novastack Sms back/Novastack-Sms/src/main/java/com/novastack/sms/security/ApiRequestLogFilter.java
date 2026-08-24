package com.novastack.sms.security;

import com.novastack.sms.domain.enums.ApiPermission;
import com.novastack.sms.domain.enums.ApiRequestOutcome;
import com.novastack.sms.usage.ApiRequestLoggedEvent;
import com.novastack.sms.usage.ApiRequestPathNormalizer;
import com.novastack.sms.usage.SensitiveDataMasker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * Records scoped API-client HTTP usage after the response is committed.
 * Does not read bodies or API keys. Persistence is asynchronous.
 */
@Component
@RequiredArgsConstructor
public class ApiRequestLogFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final ApplicationEventPublisher eventPublisher;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return !path.contains("/api/v1/")
                || path.contains("/api/v1/auth")
                || path.contains("/api/v1/dlr")
                || path.contains("/api/v1/payments")
                || path.contains("/api/v1/mpesa/stk/callback")
                || path.contains("/api/v1/mpesa/c2b/confirmation")
                || path.contains("/api/v1/mpesa/c2b/validation")
                || path.contains("/api/v1/data-bundles/callback");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        response.setHeader(REQUEST_ID_HEADER, requestId);
        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            publish(request, response, requestId, started);
        }
    }

    private void publish(
            HttpServletRequest request,
            HttpServletResponse response,
            String requestId,
            long startedNanos) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
                return;
            }
            if (!principal.isScopedApiClient() || principal.getOrganizationId() == null) {
                return;
            }
            String path = ApiRequestPathNormalizer.normalize(request.getRequestURI());
            String method = request.getMethod() == null ? "GET" : request.getMethod().toUpperCase();
            ApiPermission permission = ApiPermissionFilter.requiredPermission(method, path);
            int status = response.getStatus();
            int durationMs = (int) Math.min(Integer.MAX_VALUE, Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000L));
            eventPublisher.publishEvent(new ApiRequestLoggedEvent(
                    principal.getApiClientId(),
                    principal.getOrganizationId(),
                    requestId,
                    method,
                    path,
                    permission == null ? null : permission.name(),
                    ApiRequestPathNormalizer.category(path),
                    status,
                    outcome(status),
                    durationMs,
                    SensitiveDataMasker.clientIp(request.getHeader("X-Forwarded-For"), request.getRemoteAddr()),
                    SensitiveDataMasker.userAgent(request.getHeader("User-Agent")),
                    Instant.now()));
        } catch (Exception ignored) {
            // Analytics must never fail the API response.
        }
    }

    static ApiRequestOutcome outcome(int status) {
        if (status >= 500) {
            return ApiRequestOutcome.SERVER_ERROR;
        }
        if (status >= 400) {
            return ApiRequestOutcome.CLIENT_ERROR;
        }
        return ApiRequestOutcome.SUCCESS;
    }
}
