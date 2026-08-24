package com.novastack.sms.security;

import com.novastack.sms.domain.entity.ApiClient;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.enums.ApiRequestOutcome;
import com.novastack.sms.usage.ApiRequestLoggedEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiRequestLogFilterTest {

    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publishesScopedClientRequestWithoutBody() throws Exception {
        UUID orgId = UUID.randomUUID();
        Organization org = Organization.builder().id(orgId).name("Mwalimu").build();
        ApiClient client = ApiClient.builder()
                .id(UUID.randomUUID())
                .name("Mwalimu Production")
                .clientCode("mwalimu-production")
                .organization(org)
                .apiKeyPrefix("nova_live_")
                .build();
        var principal = UserPrincipal.fromApiClient(client);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(request.getRequestURI()).thenReturn("/api/v1/mpesa/stkpush");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("41.90.1.2");
        when(request.getHeader("User-Agent")).thenReturn("Spring Boot");
        when(response.getStatus()).thenReturn(200);

        new ApiRequestLogFilter(publisher).doFilterInternal(request, response, chain);

        ArgumentCaptor<ApiRequestLoggedEvent> captor = ArgumentCaptor.forClass(ApiRequestLoggedEvent.class);
        verify(publisher).publishEvent(captor.capture());
        ApiRequestLoggedEvent event = captor.getValue();
        assertEquals(client.getId(), event.apiClientId());
        assertEquals(orgId, event.organizationId());
        assertEquals("POST", event.method());
        assertEquals("/api/v1/mpesa/stkpush", event.path());
        assertEquals("MPESA_STK_PUSH", event.permission());
        assertEquals("MPESA", event.resourceCategory());
        assertEquals(200, event.status());
        assertEquals(ApiRequestOutcome.SUCCESS, event.outcome());
        assertEquals("41.90.1.2", event.ipAddress());
        assertEquals("Spring Boot", event.userAgent());
        assertEquals(true, event.requestId().startsWith("req_"));
    }

    @Test
    void skipsJwtDashboardUsers() throws Exception {
        var principal = UserPrincipal.fromUser(com.novastack.sms.domain.entity.User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .password("x")
                .fullName("Admin")
                .role(com.novastack.sms.domain.enums.UserRole.ORGANIZATION_ADMIN)
                .enabled(true)
                .build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        new ApiRequestLogFilter(publisher).doFilterInternal(request, response, chain);
        verifyNoInteractions(publisher);
    }

    @Test
    void outcomeBuckets() {
        assertEquals(ApiRequestOutcome.SUCCESS, ApiRequestLogFilter.outcome(201));
        assertEquals(ApiRequestOutcome.CLIENT_ERROR, ApiRequestLogFilter.outcome(403));
        assertEquals(ApiRequestOutcome.SERVER_ERROR, ApiRequestLogFilter.outcome(502));
    }
}
