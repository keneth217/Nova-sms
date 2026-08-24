package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.ApiClient;
import com.novastack.sms.domain.entity.ApiRequestLog;
import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.entity.Wallet;
import com.novastack.sms.domain.enums.ApiClientStatus;
import com.novastack.sms.domain.enums.ApiRequestOutcome;
import com.novastack.sms.domain.enums.MessageChannel;
import com.novastack.sms.domain.enums.MessageStatus;
import com.novastack.sms.domain.enums.WalletTransactionType;
import com.novastack.sms.domain.repository.ApiClientRepository;
import com.novastack.sms.domain.repository.ApiRequestLogRepository;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.domain.repository.WalletRepository;
import com.novastack.sms.domain.repository.WalletTransactionRepository;
import com.novastack.sms.dto.request.SendSmsRequest;
import com.novastack.sms.dto.response.ApiClientUsageOverviewResponse;
import com.novastack.sms.dto.response.ApiClientUsageResponse;
import com.novastack.sms.dto.response.ApiRequestLogResponse;
import com.novastack.sms.dto.response.DeveloperConfigResponse;
import com.novastack.sms.dto.response.SmsMessageResponse;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
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
    private final WalletTransactionRepository walletTransactionRepository;
    private final ApiRequestLogRepository apiRequestLogRepository;
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

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfToday = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
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

        Instant startOfWeek = today.with(DayOfWeek.MONDAY).atStartOfDay().toInstant(ZoneOffset.UTC);
        long requestsToday = apiRequestLogRepository.countByApiClientIdAndCreatedAtGreaterThanEqual(id, startOfToday);
        long requestsWeek = apiRequestLogRepository.countByApiClientIdAndCreatedAtGreaterThanEqual(id, startOfWeek);
        long requestsMonth = apiRequestLogRepository.countByApiClientIdAndCreatedAtGreaterThanEqual(id, startOfMonth);
        long successfulToday = apiRequestLogRepository.countByApiClientIdAndOutcomeAndCreatedAtGreaterThanEqual(
                id, ApiRequestOutcome.SUCCESS, startOfToday);
        long failedToday = Math.max(0, requestsToday - successfulToday);
        long smsApiToday = apiRequestLogRepository.countByApiClientIdAndResourceCategoryAndCreatedAtGreaterThanEqual(
                id, "SMS", startOfToday);
        long mpesaApiToday = apiRequestLogRepository.countByApiClientIdAndResourceCategoryAndCreatedAtGreaterThanEqual(
                id, "MPESA", startOfToday);
        long http5xxMonth = apiRequestLogRepository.countByApiClientIdAndStatusGreaterThanEqualAndCreatedAtGreaterThanEqual(
                id, 500, startOfMonth);
        long successfulMonth = apiRequestLogRepository.countByApiClientIdAndOutcomeAndCreatedAtGreaterThanEqual(
                id, ApiRequestOutcome.SUCCESS, startOfMonth);
        long failedMonth = Math.max(0, requestsMonth - successfulMonth);
        long http4xxMonth = Math.max(0, failedMonth - http5xxMonth);
        Double avgMs = apiRequestLogRepository.averageDurationMs(id, startOfMonth);
        long smsSendMonth = apiRequestLogRepository.countByApiClientIdAndPathAndCreatedAtGreaterThanEqual(
                id, "/api/v1/sms/send", startOfMonth);
        long smsBulkMonth = apiRequestLogRepository.countByApiClientIdAndPathAndCreatedAtGreaterThanEqual(
                id, "/api/v1/sms/bulk", startOfMonth);
        long stkMonth = apiRequestLogRepository.countByApiClientIdAndPathAndCreatedAtGreaterThanEqual(
                id, "/api/v1/mpesa/stkpush", startOfMonth)
                + apiRequestLogRepository.countByApiClientIdAndPathAndCreatedAtGreaterThanEqual(
                id, "/api/v1/mpesa/checkout", startOfMonth)
                + apiRequestLogRepository.countByApiClientIdAndPathAndCreatedAtGreaterThanEqual(
                id, "/api/v1/wallet/topup", startOfMonth);
        long mpesaStatusMonth = apiRequestLogRepository.countMpesaStatusCalls(id, startOfMonth);
        long c2bVerifyMonth = apiRequestLogRepository.countByApiClientIdAndPathAndCreatedAtGreaterThanEqual(
                id, "/api/v1/mpesa/c2b/verify", startOfMonth);
        long stkInitiated = walletTransactionRepository
                .countByApiClientIdAndTypeAndCheckoutRequestIdIsNotNull(id, WalletTransactionType.TOPUP);
        long stkSuccessful = walletTransactionRepository
                .countByApiClientIdAndTypeAndWalletCreditedTrue(id, WalletTransactionType.TOPUP);

        List<ApiRequestLog> monthLogs = apiRequestLogRepository
                .findByApiClientIdAndCreatedAtGreaterThanEqual(id, chartFrom);
        Map<String, long[]> httpBuckets = new LinkedHashMap<>();
        Map<String, long[]> durationBuckets = new LinkedHashMap<>();
        for (int i = 13; i >= 0; i--) {
            String day = today.minusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            httpBuckets.put(day, new long[] {0, 0, 0, 0, 0});
            durationBuckets.put(day, new long[] {0, 0});
        }
        for (ApiRequestLog row : monthLogs) {
            if (row.getCreatedAt() == null) {
                continue;
            }
            String day = LocalDate.ofInstant(row.getCreatedAt(), ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
            long[] counts = httpBuckets.get(day);
            long[] duration = durationBuckets.get(day);
            if (counts == null) {
                continue;
            }
            counts[0]++;
            if (row.getOutcome() == ApiRequestOutcome.SUCCESS) {
                counts[1]++;
            } else {
                counts[2]++;
            }
            if ("SMS".equals(row.getResourceCategory())) {
                counts[3]++;
            }
            if ("MPESA".equals(row.getResourceCategory())) {
                counts[4]++;
            }
            duration[0] += row.getDurationMs();
            duration[1]++;
        }
        List<ApiClientUsageResponse.RequestDailyPoint> requestDaily = new ArrayList<>();
        httpBuckets.forEach((date, counts) -> {
            long[] duration = durationBuckets.get(date);
            double avg = duration[1] == 0 ? 0 : (double) duration[0] / duration[1];
            requestDaily.add(ApiClientUsageResponse.RequestDailyPoint.builder()
                    .date(date)
                    .requests(counts[0])
                    .success(counts[1])
                    .failed(counts[2])
                    .sms(counts[3])
                    .mpesa(counts[4])
                    .averageDurationMs(Math.round(avg * 10.0) / 10.0)
                    .build());
        });

        List<ApiClientUsageResponse.EndpointCount> topEndpoints = new ArrayList<>();
        for (Object[] row : apiRequestLogRepository.topPaths(id, startOfMonth, PageRequest.of(0, 8))) {
            topEndpoints.add(ApiClientUsageResponse.EndpointCount.builder()
                    .path(String.valueOf(row[0]))
                    .count(((Number) row[1]).longValue())
                    .build());
        }

        double successRate = requestsMonth == 0 ? 0 : (successfulMonth * 100.0) / requestsMonth;

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
                .requestsToday(requestsToday)
                .requestsThisWeek(requestsWeek)
                .requestsThisMonth(requestsMonth)
                .successfulToday(successfulToday)
                .failedToday(failedToday)
                .smsApiCallsToday(smsApiToday)
                .mpesaApiCallsToday(mpesaApiToday)
                .smsSendCallsThisMonth(smsSendMonth)
                .smsBulkCallsThisMonth(smsBulkMonth)
                .mpesaStkCallsThisMonth(stkMonth)
                .mpesaStatusCallsThisMonth(mpesaStatusMonth)
                .c2bVerifyCallsThisMonth(c2bVerifyMonth)
                .mpesaStkInitiated(stkInitiated)
                .mpesaStkSuccessful(stkSuccessful)
                .successRateThisMonth(Math.round(successRate * 10.0) / 10.0)
                .averageDurationMsThisMonth(avgMs == null ? null : Math.round(avgMs * 10.0) / 10.0)
                .http4xxThisMonth(http4xxMonth)
                .http5xxThisMonth(http5xxMonth)
                .requestDaily(requestDaily)
                .topEndpoints(topEndpoints)
                .build();
    }

    @Transactional(readOnly = true)
    public ApiClientUsageOverviewResponse usageOverview() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfToday = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant startOfWeek = today.with(DayOfWeek.MONDAY).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<ApiClient> clients = apiClientRepository.findAllWithOrganization();
        Instant now = Instant.now();
        Map<UUID, long[]> todayByClient = new HashMap<>();
        for (Object[] row : apiRequestLogRepository.requestCountsByClient(startOfToday)) {
            todayByClient.put((UUID) row[0], new long[] {((Number) row[1]).longValue(), 0, 0, 0});
        }
        for (Object[] row : apiRequestLogRepository.outcomeCountsByClient(startOfToday)) {
            UUID clientId = (UUID) row[0];
            ApiRequestOutcome outcome = (ApiRequestOutcome) row[1];
            long count = ((Number) row[2]).longValue();
            long[] bucket = todayByClient.computeIfAbsent(clientId, key -> new long[] {0, 0, 0, 0});
            if (outcome == ApiRequestOutcome.SUCCESS) {
                bucket[1] += count;
            } else {
                bucket[2] += count;
            }
        }
        for (Object[] row : apiRequestLogRepository.categoryCountsByClient(startOfToday)) {
            UUID clientId = (UUID) row[0];
            String category = String.valueOf(row[1]);
            long count = ((Number) row[2]).longValue();
            long[] bucket = todayByClient.computeIfAbsent(clientId, key -> new long[] {0, 0, 0, 0});
            if ("MPESA".equals(category)) {
                bucket[3] += count;
            }
        }

        List<ApiClientUsageOverviewResponse.ClientCard> cards = new ArrayList<>();
        long platformToday = 0;
        for (ApiClient client : clients) {
            long[] todayStats = todayByClient.getOrDefault(client.getId(), new long[] {0, 0, 0, 0});
            platformToday += todayStats[0];
            String orgName = client.getOrganization() == null ? null : client.getOrganization().getName();
            cards.add(ApiClientUsageOverviewResponse.ClientCard.builder()
                    .id(client.getId())
                    .name(client.getName())
                    .organizationName(orgName)
                    .status(client.getStatus() == null ? null : client.getStatus().name())
                    .requestsToday(todayStats[0])
                    .successfulToday(todayStats[1])
                    .failedToday(todayStats[2])
                    .smsSent(smsMessageRepository.countByApiClientIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                            client.getId(), startOfToday, now.plusSeconds(1)))
                    .mpesaRequestsToday(todayStats[3])
                    .lastRequestAt(client.getLastUsedAt())
                    .build());
        }
        cards.sort((a, b) -> Long.compare(b.getRequestsToday(), a.getRequestsToday()));

        Map<UUID, Long> monthCounts = new LinkedHashMap<>();
        for (Object[] row : apiRequestLogRepository.requestCountsByClient(startOfMonth)) {
            monthCounts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        List<ApiClientUsageOverviewResponse.ClientRank> ranks = new ArrayList<>();
        for (ApiClient client : clients) {
            long count = monthCounts.getOrDefault(client.getId(), 0L);
            if (count == 0) {
                continue;
            }
            String orgName = client.getOrganization() == null ? null : client.getOrganization().getName();
            ranks.add(ApiClientUsageOverviewResponse.ClientRank.builder()
                    .id(client.getId())
                    .name(client.getName())
                    .organizationName(orgName)
                    .requests(count)
                    .build());
        }
        ranks.sort((a, b) -> Long.compare(b.getRequests(), a.getRequests()));

        long weekTotal = 0;
        for (Object[] row : apiRequestLogRepository.requestCountsByClient(startOfWeek)) {
            weekTotal += ((Number) row[1]).longValue();
        }
        long monthTotal = monthCounts.values().stream().mapToLong(Long::longValue).sum();

        return ApiClientUsageOverviewResponse.builder()
                .requestsToday(platformToday)
                .requestsThisWeek(weekTotal)
                .requestsThisMonth(monthTotal)
                .clients(cards)
                .byClientThisMonth(ranks)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ApiRequestLogResponse> requestLogs(UUID clientId, Pageable pageable) {
        if (!apiClientRepository.existsById(clientId)) {
            throw new ApiException("API client not found", HttpStatus.NOT_FOUND);
        }
        return apiRequestLogRepository.findByApiClientIdOrderByCreatedAtDesc(clientId, pageable)
                .map(this::toLogResponse);
    }

    private ApiRequestLogResponse toLogResponse(ApiRequestLog log) {
        return ApiRequestLogResponse.builder()
                .id(log.getId())
                .requestId(log.getRequestId())
                .method(log.getMethod())
                .path(log.getPath())
                .permission(log.getPermission())
                .resourceCategory(log.getResourceCategory())
                .status(log.getStatus())
                .outcome(log.getOutcome())
                .durationMs(log.getDurationMs())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
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
