package com.novastack.sms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.domain.enums.ApiPermission;
import com.novastack.sms.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiPermissionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!principal.isScopedApiClient()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = normalizeApiPath(request.getRequestURI());
        String method = request.getMethod();
        if (path == null) {
            filterChain.doFilter(request, response);
            return;
        }

        ApiPermission required = requiredPermission(method, path);
        if (required == null) {
            writeForbidden(response, "This API key cannot access that resource");
            return;
        }
        if (!principal.hasPermission(required)) {
            writeForbidden(response, "API key is missing permission " + required.name());
            return;
        }

        filterChain.doFilter(request, response);
    }

    static String normalizeApiPath(String uri) {
        if (uri == null) {
            return null;
        }
        int idx = uri.indexOf("/api/v1/");
        return idx >= 0 ? uri.substring(idx) : uri;
    }

    /**
     * Required permission for a scoped live key, or {@code null} if the path is not allowed.
     */
    static ApiPermission requiredPermission(String method, String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("/api/v1/sms")) {
            return requiredSmsPermission(method, path);
        }
        if (path.startsWith("/api/v1/mpesa")) {
            return requiredMpesaPermission(method, path);
        }
        if (path.startsWith("/api/v1/wallet")) {
            return requiredWalletPermission(method, path);
        }
        return null;
    }

    private static ApiPermission requiredSmsPermission(String method, String path) {
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/sms/send")) {
            return ApiPermission.SMS_SEND;
        }
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/sms/bulk")) {
            return ApiPermission.SMS_BULK;
        }
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/sms/schedule")) {
            return ApiPermission.SMS_BULK;
        }
        if ("POST".equalsIgnoreCase(method) && path.matches("/api/v1/sms/batches/[^/]+/resend-failed")) {
            return ApiPermission.SMS_BULK;
        }
        if ("POST".equalsIgnoreCase(method) && path.matches("/api/v1/sms/[^/]+/resend")) {
            return ApiPermission.SMS_SEND;
        }
        if ("GET".equalsIgnoreCase(method) && path.matches("/api/v1/sms/batches/[^/]+$")) {
            return ApiPermission.SMS_STATUS;
        }
        if ("GET".equalsIgnoreCase(method)
                && (path.equals("/api/v1/sms") || path.equals("/api/v1/sms/") || path.endsWith("/sms/history"))) {
            return ApiPermission.SMS_HISTORY;
        }
        if ("GET".equalsIgnoreCase(method) && path.contains("/sms/") && path.endsWith("/status")) {
            return ApiPermission.SMS_STATUS;
        }
        if ("GET".equalsIgnoreCase(method) && path.matches("/api/v1/sms/[^/]+$")) {
            return ApiPermission.SMS_STATUS;
        }
        return ApiPermission.SMS_SEND;
    }

    private static ApiPermission requiredMpesaPermission(String method, String path) {
        if ("POST".equalsIgnoreCase(method)
                && (path.endsWith("/mpesa/stkpush") || path.endsWith("/mpesa/checkout"))) {
            return ApiPermission.MPESA_STK_PUSH;
        }
        if ("GET".equalsIgnoreCase(method) && path.matches("/api/v1/mpesa/checkout/[^/]+/status$")) {
            return ApiPermission.MPESA_STATUS;
        }
        if ("GET".equalsIgnoreCase(method) && path.matches("/api/v1/mpesa/checkout/[^/]+$")) {
            return ApiPermission.MPESA_STATUS;
        }
        if ("GET".equalsIgnoreCase(method) && path.matches("/api/v1/mpesa/transactions/[^/]+/status$")) {
            return ApiPermission.MPESA_STATUS;
        }
        if ("GET".equalsIgnoreCase(method) && path.matches("/api/v1/mpesa/transactions/[^/]+$")) {
            return ApiPermission.MPESA_STATUS;
        }
        if ("GET".equalsIgnoreCase(method) && path.matches("/api/v1/mpesa/c2b/transactions/[^/]+$")) {
            return ApiPermission.MPESA_C2B;
        }
        if ("GET".equalsIgnoreCase(method)
                && (path.equals("/api/v1/mpesa/c2b/transactions")
                || path.equals("/api/v1/mpesa/c2b/transactions/"))) {
            return ApiPermission.MPESA_C2B;
        }
        if ("GET".equalsIgnoreCase(method)
                && (path.equals("/api/v1/mpesa/c2b") || path.equals("/api/v1/mpesa/c2b/"))) {
            return ApiPermission.MPESA_C2B;
        }
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/mpesa/c2b/verify")) {
            return ApiPermission.MPESA_C2B;
        }
        return null;
    }

    private static ApiPermission requiredWalletPermission(String method, String path) {
        if (path.contains("/wallet/topup")) {
            return ApiPermission.WALLET_TOPUP;
        }
        if ("GET".equalsIgnoreCase(method)
                && (path.endsWith("/wallet/balance") || path.contains("/wallet/transactions"))) {
            return ApiPermission.WALLET_READ;
        }
        return null;
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(message));
    }
}
