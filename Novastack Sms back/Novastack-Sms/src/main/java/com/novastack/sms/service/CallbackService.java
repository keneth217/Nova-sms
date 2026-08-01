package com.novastack.sms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Alias facade for Safaricom data-bundle asynchronous callbacks.
 * Delegates to {@link DataBundleCallbackService}.
 */
@Service
@RequiredArgsConstructor
public class CallbackService {

    private final DataBundleCallbackService dataBundleCallbackService;

    public Map<String, Object> handleCallback(String payload, String callbackToken) {
        return dataBundleCallbackService.handleCallback(payload, callbackToken);
    }
}
