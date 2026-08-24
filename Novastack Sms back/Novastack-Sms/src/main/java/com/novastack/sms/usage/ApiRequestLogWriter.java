package com.novastack.sms.usage;

import com.novastack.sms.domain.entity.ApiRequestLog;
import com.novastack.sms.domain.repository.ApiRequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiRequestLogWriter {

    private final ApiRequestLogRepository apiRequestLogRepository;

    @Async("apiUsageExecutor")
    @EventListener
    public void onApiRequest(ApiRequestLoggedEvent event) {
        if (event == null || event.apiClientId() == null) {
            return;
        }
        try {
            apiRequestLogRepository.save(ApiRequestLog.builder()
                    .apiClientId(event.apiClientId())
                    .organizationId(event.organizationId())
                    .requestId(event.requestId())
                    .method(event.method())
                    .path(event.path())
                    .permission(event.permission())
                    .resourceCategory(event.resourceCategory())
                    .status(event.status())
                    .outcome(event.outcome())
                    .durationMs(event.durationMs())
                    .ipAddress(event.ipAddress())
                    .userAgent(event.userAgent())
                    .createdAt(event.createdAt())
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to persist API usage log client={} path={}: {}",
                    event.apiClientId(), event.path(), ex.getMessage());
        }
    }
}
