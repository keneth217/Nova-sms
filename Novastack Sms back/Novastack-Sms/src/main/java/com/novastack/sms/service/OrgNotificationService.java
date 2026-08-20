package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.provider.SmsProvider;
import com.novastack.sms.provider.SmsProviderFactory;
import com.novastack.sms.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrgNotificationService {

    private static final int GSM_SMS_LIMIT = 160;
    private static final ZoneId NAIROBI = ZoneId.of("Africa/Nairobi");
    private static final DateTimeFormatter SMS_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final SmsProviderFactory smsProviderFactory;
    private final AppProperties appProperties;
    private final SmsSettingsService smsSettingsService;

    public void notifyWelcome(Organization organization) {
        if (!enabled(organization) || organization == null) {
            return;
        }
        String phone = organization.getPhone();
        if (blank(phone)) {
            log.warn("Skipping welcome SMS for org {} — no phone on file", organization.getId());
            return;
        }
        String message = render(smsSettingsService.welcomeTemplate(),
                "name", organization.getName(),
                "portalUrl", portalUrl());
        queue("welcome", organization, phone, message);
    }

    public void notifyTopUpSuccess(Organization organization, BigDecimal amount, BigDecimal newBalance, String mpesaReceipt) {
        if (!enabled(organization) || organization == null) {
            return;
        }
        String phone = organization.getPhone();
        if (blank(phone)) {
            log.warn("Skipping top-up SMS for org {} — no phone on file", organization.getId());
            return;
        }
        String receiptPart = blank(mpesaReceipt) ? "" : " Receipt " + mpesaReceipt + ".";
        String message = render(smsSettingsService.topupTemplate(),
                "amount", kes(amount),
                "receipt", receiptPart,
                "balance", kes(newBalance));
        queue("topup", organization, phone, message);
    }

    public void notifyPlatformOwnerTopUp(
            Organization organization, BigDecimal amount, BigDecimal newBalance, String mpesaReceipt) {
        if (!smsSettingsService.isEnabled() || organization == null) {
            return;
        }
        List<String> phones = notifyPhones();
        if (phones.isEmpty()) {
            log.warn("Skipping platform-owner top-up SMS for org {} — no collection-notify-phones configured",
                    organization.getId());
            return;
        }
        String name = blank(organization.getName()) ? "Organization" : organization.getName().trim();
        String account = blank(organization.getMpesaAccountRef()) ? "" : organization.getMpesaAccountRef().trim();
        String receiptPart = blank(mpesaReceipt) ? "" : " Ref " + mpesaReceipt.trim() + ".";
        String credited = kesGrouped(amount);
        String balance = kesGrouped(newBalance);
        String time = nairobiTime();
        String message = renderPlatformTopUp(name, account, credited, receiptPart, balance, time);
        if (message.length() > GSM_SMS_LIMIT) {
            int overflow = message.length() - GSM_SMS_LIMIT;
            String shortened = trimTo(name, Math.max(8, name.length() - overflow));
            message = renderPlatformTopUp(shortened, account, credited, receiptPart, balance, time);
        }
        message = capGsm(message);
        for (String phone : phones) {
            queuePlatform("platform-topup", phone, message);
        }
    }

    public void notifyProviderLowUnits(BigDecimal remainingUnits, BigDecimal threshold) {
        if (!smsSettingsService.isEnabled()) {
            return;
        }
        List<String> phones = notifyPhones();
        if (phones.isEmpty()) {
            log.warn("Skipping TalkSasa units alert — no collection-notify-phones configured");
            return;
        }
        String units = kesGrouped(remainingUnits);
        String limit = kesGrouped(threshold);
        String message = capGsm(render(smsSettingsService.providerLowTemplate(),
                "units", units,
                "threshold", limit));
        for (String phone : phones) {
            queuePlatform("provider-low", phone, message);
        }
    }

    public void notifyWalletExposure(BigDecimal totalWallets, BigDecimal remainingUnits) {
        if (!smsSettingsService.isEnabled()) {
            return;
        }
        List<String> phones = notifyPhones();
        if (phones.isEmpty()) {
            log.warn("Skipping wallet-exposure SMS — no collection-notify-phones configured");
            return;
        }
        String wallets = kesGrouped(totalWallets);
        String units = kesGrouped(remainingUnits);
        String message = capGsm(render(smsSettingsService.providerExposureTemplate(),
                "wallets", wallets,
                "units", units));
        for (String phone : phones) {
            queuePlatform("provider-exposure", phone, message);
        }
    }

    private String renderPlatformTopUp(
            String name, String account, String amount, String receiptPart, String balance, String time) {
        String message = render(smsSettingsService.platformTopupTemplate(),
                "name", name,
                "account", account,
                "amount", amount,
                "receipt", receiptPart,
                "balance", balance,
                "time", time);
        return message.replace(" ()", "");
    }

    public void notifyCollectionReceived(
            String billRef,
            BigDecimal amount,
            String firstName,
            String middleName,
            String lastName,
            String mpesaReceipt) {
        if (!smsSettingsService.isEnabled()) {
            return;
        }
        List<String> phones = notifyPhones();
        if (phones.isEmpty()) {
            log.warn("Skipping collection SMS for {} — no collection-notify-phones configured", billRef);
            return;
        }
        String payer = joinName(firstName, middleName, lastName);
        String from = payer == null ? "a customer" : payer;
        String destination = collectionDisplayName(billRef);
        String receiptPart = blank(mpesaReceipt) ? "" : " Receipt " + mpesaReceipt + ".";
        String message = render(smsSettingsService.collectionTemplate(),
                "amount", kes(amount),
                "payer", from,
                "account", destination,
                "receipt", receiptPart);
        for (String phone : phones) {
            queuePlatform("collection", phone, message);
        }
    }

    public void notifyLowBalance(Organization organization, BigDecimal balance) {
        if (!enabled(organization) || organization == null) {
            return;
        }
        String phone = organization.getPhone();
        if (blank(phone)) {
            log.warn("Skipping low-balance SMS for org {} — no phone on file", organization.getId());
            return;
        }
        String message = render(smsSettingsService.lowBalanceTemplate(),
                "balance", kes(balance));
        queue("low-balance", organization, phone, message);
    }

    public boolean crossedLowBalanceThreshold(Organization organization, BigDecimal before, BigDecimal after) {
        return crossedLowBalanceThreshold(before, after, resolveThreshold(organization));
    }

    public boolean crossedLowBalanceThreshold(BigDecimal before, BigDecimal after, BigDecimal threshold) {
        if (threshold == null || before == null || after == null) {
            return false;
        }
        return before.compareTo(threshold) > 0 && after.compareTo(threshold) <= 0;
    }

    public BigDecimal resolveThreshold(Organization organization) {
        if (organization != null && organization.getLowBalanceThreshold() != null) {
            return organization.getLowBalanceThreshold();
        }
        BigDecimal fallback = smsSettingsService.lowBalanceThreshold();
        return fallback != null ? fallback : new BigDecimal("50.00");
    }

    private void queue(String kind, Organization organization, String phone, String message) {
        SmsProvider.SmsProviderRequest request = smsProviderFactory.buildRequest(
                organization, phone, message, platformSender());
        UUID orgId = organization.getId();
        runAfterCommit(() -> send(kind, orgId, request));
    }

    private void queuePlatform(String kind, String phone, String message) {
        SmsProvider.SmsProviderRequest request = smsProviderFactory.buildRequest(
                null, phone, message, platformSender());
        runAfterCommit(() -> send(kind, null, request));
    }

    private List<String> notifyPhones() {
        List<String> configured = smsSettingsService.collectionNotifyPhones();
        if (configured == null || configured.isEmpty()) {
            return List.of();
        }
        List<String> phones = new ArrayList<>();
        for (String raw : configured) {
            if (blank(raw)) {
                continue;
            }
            String normalized = PhoneNormalizer.normalize(raw.trim());
            if (PhoneNormalizer.isKenyanMobile(normalized) && !phones.contains(normalized)) {
                phones.add(normalized);
            } else {
                log.warn("Ignoring invalid collection notify phone '{}'", raw);
            }
        }
        return phones;
    }

    private static String collectionDisplayName(String billRef) {
        if (billRef == null || billRef.isBlank()) {
            return "";
        }
        String trimmed = billRef.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    private static String joinName(String firstName, String middleName, String lastName) {
        String joined = String.join(" ",
                firstName == null ? "" : firstName.trim(),
                middleName == null ? "" : middleName.trim(),
                lastName == null ? "" : lastName.trim())
                .replaceAll("\\s+", " ")
                .trim();
        return joined.isEmpty() ? null : joined;
    }

    private void send(String kind, UUID organizationId, SmsProvider.SmsProviderRequest request) {
        try {
            SmsProvider.SmsProviderResult result = smsProviderFactory.getDefaultProvider().send(request);
            if (result.success()) {
                log.info("Org {} SMS sent kind={} to={} providerMessageId={}",
                        organizationId, kind, request.recipient(), result.providerMessageId());
            } else {
                log.warn("Org {} SMS failed kind={} to={} error={}",
                        organizationId, kind, request.recipient(), result.errorMessage());
            }
        } catch (Exception ex) {
            log.warn("Org {} SMS failed kind={} to={}: {}",
                    organizationId, kind, request.recipient(), ex.getMessage());
        }
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private boolean enabled(Organization organization) {
        if (!smsSettingsService.isEnabled()) {
            return false;
        }
        return organization == null || organization.isNotificationsEnabled();
    }

    static String render(String template, String... keysAndValues) {
        String result = template == null ? "" : template;
        if (keysAndValues == null) {
            return result;
        }
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            String key = keysAndValues[i];
            String value = keysAndValues[i + 1] == null ? "" : keysAndValues[i + 1];
            result = result.replace("{" + key + "}", value);
        }
        return result;
    }

    private String portalUrl() {
        String url = smsSettingsService.portalUrl();
        return blank(url) ? "https://novasms.novastack.co.ke" : url.trim();
    }

    private String platformSender() {
        String provider = appProperties.getSms().getProvider();
        if (provider == null || provider.isBlank() || "talksasa".equalsIgnoreCase(provider.trim())) {
            return appProperties.getSms().getTalksasa().resolvedDefaultSenderId();
        }
        return appProperties.getSms().getPlatformSenderId();
    }

    private static String kes(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String kesGrouped(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        BigDecimal value = amount.stripTrailingZeros();
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat format = value.scale() <= 0
                ? new DecimalFormat("#,##0", symbols)
                : new DecimalFormat("#,##0.00", symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(amount);
    }

    private static String nairobiTime() {
        return ZonedDateTime.now(NAIROBI).format(SMS_TIME);
    }

    private static String trimTo(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxChars).trim();
    }

    private static String capGsm(String message) {
        if (message == null) {
            return "";
        }
        return message.length() <= GSM_SMS_LIMIT ? message : message.substring(0, GSM_SMS_LIMIT);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
