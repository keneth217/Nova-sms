package com.novastack.sms.provider;

import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmsProviderFactory {

    private final AfricasTalkingSmsProvider africasTalkingSmsProvider;
    private final AppProperties appProperties;

    public SmsProvider getDefaultProvider() {
        return africasTalkingSmsProvider;
    }

    public SmsProvider.SmsProviderRequest buildRequest(Organization org, String recipient, String message, String senderId) {
        return new SmsProvider.SmsProviderRequest(
                org.getAtUsername(),
                org.getAtApiKey(),
                recipient,
                message,
                senderId,
                appProperties.getAfricastalking().getBaseUrl()
        );
    }
}
