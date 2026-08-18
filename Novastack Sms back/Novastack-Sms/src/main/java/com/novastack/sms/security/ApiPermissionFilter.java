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

        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!path.startsWith("/api/v1/sms")) {
            writeForbidden(response, "This API key cannot access that resource");
            return;
        }

        ApiPermission required = requiredPermission(method, path);
        if (required != null && !principal.hasPermission(required)) {
            writeForbidden(response, "API key is missing permission " + required.name());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static ApiPermission requiredPermission(String method, String path) {
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/sms/send")) {
            return ApiPermission.SMS_SEND;
        }
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/sms/bulk")) {
            return ApiPermission.SMS_BULK;
        }
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/sms/schedule")) {
            return ApiPermission.SMS_BULK;
        }
        if ("GET".equalsIgnoreCase(method) && path.endsWith("/sms/history")) {
            return ApiPermission.SMS_HISTORY;
        }
        if ("GET".equalsIgnoreCase(method) && path.contains("/sms/") && path.endsWith("/status")) {
            return ApiPermission.SMS_STATUS;
        }
        if ("GET".equalsIgnoreCase(method) && path.matches(".*/api/v1/sms/[^/]+$")) {
            return ApiPermission.SMS_STATUS;
        }
        return ApiPermission.SMS_SEND;
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(message));
    }
}
