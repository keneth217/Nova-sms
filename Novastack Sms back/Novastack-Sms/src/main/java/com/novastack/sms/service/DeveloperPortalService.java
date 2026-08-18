package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.ApiClient;
import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.entity.Wallet;
import com.novastack.sms.domain.enums.ApiClientStatus;
import com.novastack.sms.domain.enums.MessageChannel;
import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.domain.repository.ApiClientRepository;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.domain.repository.WalletRepository;
import com.novastack.sms.dto.request.SendSmsRequest;
import com.novastack.sms.dto.response.ApiClientUsageResponse;
import com.novastack.sms.dto.response.DeveloperConfigResponse;
import com.novastack.sms.dto.response.SmsMessageResponse;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeveloperPortalService {

    private static final EnumSet<MessageStatus> FAILED_STATUSES = EnumSet.of(
            MessageStatus.FAILED, MessageStatus.REJECTED, MessageStatus.CANCELLED);

    private final AppProperties appProperties;
    private final ApiClientRepository apiClientRepository;
    private final ApiClientService apiClientService;
    private final SmsMessageRepository smsMessageRepository;
    private final WalletRepository walletRepository;
    private final SmsService smsService;

    @Transactional(readOnly = true)
    public DeveloperConfigResponse publicConfig() {
        String origin = trimSlash(appProperties.getApi().getPublicBaseUrl());
        return DeveloperConfigResponse.builder()
                .publicBaseUrl(origin)
                .apiBaseUrl(origin + "/api/v1")
                .openApiPath("/v3/api-docs")
                .swaggerUiPath("/swagger-ui.html")
                .build();
    }

    @Transactional(readOnly = true)
    public ApiClientUsageResponse usage(UUID clientId) {
        ApiClient client = apiClientRepository.findById(clientId)
                .orElseThrow(() -> new ApiException("API client not found", HttpStatus.NOT_FOUND));
        UUID id = client.getId();
        long total = smsMessageRepository.countByApiClientId(id);
        long failed = smsMessageRepository.countByApiClientIdAndStatusIn(id, FAILED_STATUSES);
        long units = smsMessageRepository.sumUnitsByApiClientId(id);

        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant startOfMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1)
                .atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant now = Instant.now();

        Wallet wallet = walletRepository.findByOrganizationId(client.getOrganization().getId()).orElse(null);
        Instant lastSms = smsMessageRepository.findFirstByApiClientIdOrderByCreatedAtDesc(id)
                .map(SmsMessage::getCreatedAt)
                .orElse(null);

        Instant chartFrom = LocalDate.now(ZoneOffset.UTC).minusDays(13).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<SmsMessage> recent = smsMessageRepository
                .findByApiClientIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(id, chartFrom);

        Map<String, long[]> buckets = new LinkedHashMap<>();
        for (int i = 13; i >= 0; i--) {
            String day = LocalDate.now(ZoneOffset.UTC).minusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            buckets.put(day, new long[] {0, 0, 0});
        }
        for (SmsMessage message : recent) {
            if (message.getCreatedAt() == null) {
                continue;
            }
            String day = LocalDate.ofInstant(message.getCreatedAt(), ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
            long[] counts = buckets.get(day);
            if (counts == null) {
                continue;
            }
            counts[0]++;
            if (message.getStatus() == MessageStatus.DELIVERED) {
                counts[1]++;
            }
            if (FAILED_STATUSES.contains(message.getStatus())) {
                counts[2]++;
            }
        }

        List<ApiClientUsageResponse.DailyPoint> daily = new ArrayList<>();
        buckets.forEach((date, counts) -> daily.add(ApiClientUsageResponse.DailyPoint.builder()
                .date(date)
                .sent(counts[0])
                .delivered(counts[1])
                .failed(counts[2])
                .build()));

        return ApiClientUsageResponse.builder()
                .client(apiClientService.toResponse(client))
                .totalSms(total)
                .successfulSms(Math.max(0, total - failed))
                .failedSms(failed)
                .smsUnitsUsed(units)
                .walletBalance(wallet != null ? wallet.getBalance() : BigDecimal.ZERO)
                .walletCurrency(wallet != null ? wallet.getCurrency() : "KES")
                .lastRequestAt(client.getLastUsedAt())
                .lastSmsAt(lastSms)
                .smsToday(smsMessageRepository.countByApiClientIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        id, startOfToday, now.plusSeconds(1)))
                .smsThisMonth(smsMessageRepository.countByApiClientIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        id, startOfMonth, now.plusSeconds(1)))
                .daily(daily)
                .build();
    }

    @Transactional
    public SmsMessageResponse testSend(UUID clientId, SendSmsRequest request) {
        ApiClient client = apiClientRepository.findById(clientId)
                .orElseThrow(() -> new ApiException("API client not found", HttpStatus.NOT_FOUND));
        if (client.getStatus() != ApiClientStatus.ACTIVE) {
            throw new ApiException("API client is not active", HttpStatus.BAD_REQUEST);
        }
        return smsService.sendSingle(
                client.getOrganization().getId(),
                request,
                MessageChannel.SMS,
                client.getId());
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "https://smsapi.novastack.co.ke";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url.trim();
    }
}
