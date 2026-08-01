package com.novastack.sms.provider;

import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsProviderFactory {

    private final AfricasTalkingSmsProvider africasTalkingSmsProvider;
    private final AppProperties appProperties;

    public SmsProvider getDefaultProvider() {
        return africasTalkingSmsProvider;
    }

    public SmsProvider.SmsProviderRequest buildRequest(Organization org, String recipient, String message, String senderId) {
        AtCredentials credentials = resolveAtCredentials(org);
        return new SmsProvider.SmsProviderRequest(
                credentials.username(),
                credentials.apiKey(),
                recipient,
                message,
                senderId,
                credentials.baseUrl()
        );
    }

    public SmsProvider.SmsBulkRequest buildBulkRequest(
            Organization org,
            java.util.Collection<String> recipients,
            String message,
            String senderId) {
        AtCredentials credentials = resolveAtCredentials(org);
        return new SmsProvider.SmsBulkRequest(
                credentials.username(),
                credentials.apiKey(),
                recipients,
                message,
                senderId,
                credentials.baseUrl()
        );
    }

    /**
     * Platform sandbox/live settings win while testing with username {@code sandbox}.
     * Per-org {@code at_username}/{@code at_api_key} are used only when both are set
     * and the platform is not in sandbox mode (so sender ID like NOVASTACK is never
     * mistaken for the AT account username).
     */
    private AtCredentials resolveAtCredentials(Organization org) {
        AppProperties.AfricasTalking platform = appProperties.getAfricastalking();
        boolean sandboxMode = isSandboxMode(platform);

        if (sandboxMode) {
            if (hasText(org.getAtUsername())
                    && !"sandbox".equalsIgnoreCase(org.getAtUsername().trim())) {
                log.warn(
                        "Ignoring org AT username '{}' while platform is in sandbox mode; using username 'sandbox'",
                        org.getAtUsername());
            }
            return new AtCredentials(null, null, platform.getBaseUrl());
        }

        if (hasText(org.getAtUsername()) && hasText(org.getAtApiKey())) {
            return new AtCredentials(org.getAtUsername().trim(), org.getAtApiKey().trim(), platform.getBaseUrl());
        }

        if (hasText(org.getAtUsername()) || hasText(org.getAtApiKey())) {
            log.warn(
                    "Organization {} has incomplete AT credentials; falling back to platform AT settings",
                    org.getId());
        }

        return new AtCredentials(null, null, platform.getBaseUrl());
    }

    private boolean isSandboxMode(AppProperties.AfricasTalking platform) {
        if ("sandbox".equalsIgnoreCase(blank(platform.getUsername()))) {
            return true;
        }
        String baseUrl = blank(platform.getBaseUrl());
        return baseUrl != null && baseUrl.contains("sandbox");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record AtCredentials(String username, String apiKey, String baseUrl) {
    }
}
