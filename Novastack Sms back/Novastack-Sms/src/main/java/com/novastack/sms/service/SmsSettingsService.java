package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.PlatformSmsSettings;
import com.novastack.sms.domain.repository.PlatformSmsSettingsRepository;
import com.novastack.sms.dto.request.UpdatePlatformSmsSettingsRequest;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SmsSettingsService {

    private static final Pattern ACCOUNT_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{1,31}$");
    private static final String DEFAULT_ACCOUNTS = "SHEILA,KENETH";
    private static final String DEFAULT_PHONES = "0711766223,0759728742";

    private final PlatformSmsSettingsRepository repository;
    private final AppProperties appProperties;

    @Transactional
    public PlatformSmsSettings current() {
        return repository.findById(PlatformSmsSettings.SINGLETON_ID).orElseGet(this::seedFromConfig);
    }

    @Transactional
    public boolean isEnabled() {
        return current().isEnabled();
    }

    @Transactional
    public BigDecimal lowBalanceThreshold() {
        BigDecimal value = current().getLowBalanceThreshold();
        if (value != null) {
            return value;
        }
        BigDecimal yaml = appProperties.getNotifications().getLowBalanceThreshold();
        return yaml != null ? yaml : new BigDecimal("50.00");
    }

    @Transactional
    public String portalUrl() {
        return firstNonBlank(current().getPortalUrl(), yamlPortalUrl());
    }

    @Transactional
    public String welcomeTemplate() {
        return firstNonBlank(current().getTemplateWelcome(), yamlTemplates().getWelcome());
    }

    @Transactional
    public String topupTemplate() {
        return firstNonBlank(current().getTemplateTopup(), yamlTemplates().getTopup());
    }

    @Transactional
    public String collectionTemplate() {
        return firstNonBlank(current().getTemplateCollection(), yamlTemplates().getCollection());
    }

    @Transactional
    public String lowBalanceTemplate() {
        return firstNonBlank(current().getTemplateLowBalance(), yamlTemplates().getLowBalance());
    }

    @Transactional
    public String platformTopupTemplate() {
        return firstNonBlank(current().getTemplatePlatformTopup(), yamlTemplates().getPlatformTopup());
    }

    @Transactional
    public String providerLowTemplate() {
        return firstNonBlank(current().getTemplateProviderLow(), yamlTemplates().getProviderLow());
    }

    @Transactional
    public String providerExposureTemplate() {
        return firstNonBlank(current().getTemplateProviderExposure(), yamlTemplates().getProviderExposure());
    }

    @Transactional
    public boolean talksasaLowAlerted() {
        return current().isTalksasaLowAlerted();
    }

    @Transactional
    public boolean talksasaExposureAlerted() {
        return current().isTalksasaExposureAlerted();
    }

    @Transactional
    public void recordTalksasaAlertState(BigDecimal remaining, boolean lowAlerted, boolean exposureAlerted) {
        PlatformSmsSettings settings = current();
        settings.setTalksasaLastRemaining(remaining);
        settings.setTalksasaLowAlerted(lowAlerted);
        settings.setTalksasaExposureAlerted(exposureAlerted);
        repository.save(settings);
    }

    @Transactional
    public List<String> collectionAccounts() {
        return parseAccounts(firstNonBlank(current().getCollectionAccounts(), yamlCollectionAccounts()));
    }

    @Transactional
    public List<String> collectionNotifyPhones() {
        return parsePhones(firstNonBlank(current().getCollectionNotifyPhones(), yamlCollectionNotifyPhones()), false);
    }

    @Transactional
    public PlatformSmsSettings update(UpdatePlatformSmsSettingsRequest request) {
        if (request == null) {
            throw new ApiException("SMS settings are required", HttpStatus.BAD_REQUEST);
        }
        PlatformSmsSettings settings = current();
        if (request.getEnabled() != null) {
            settings.setEnabled(request.getEnabled());
        }
        if (request.getLowBalanceThreshold() != null) {
            settings.setLowBalanceThreshold(request.getLowBalanceThreshold().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getPortalUrl() != null && !request.getPortalUrl().isBlank()) {
            settings.setPortalUrl(request.getPortalUrl().trim());
        }
        if (notBlank(request.getWelcomeTemplate())) {
            settings.setTemplateWelcome(request.getWelcomeTemplate().trim());
        }
        if (notBlank(request.getTopupTemplate())) {
            settings.setTemplateTopup(request.getTopupTemplate().trim());
        }
        if (notBlank(request.getCollectionTemplate())) {
            settings.setTemplateCollection(request.getCollectionTemplate().trim());
        }
        if (notBlank(request.getLowBalanceTemplate())) {
            settings.setTemplateLowBalance(request.getLowBalanceTemplate().trim());
        }
        if (notBlank(request.getPlatformTopupTemplate())) {
            settings.setTemplatePlatformTopup(request.getPlatformTopupTemplate().trim());
        }
        if (notBlank(request.getProviderLowTemplate())) {
            settings.setTemplateProviderLow(request.getProviderLowTemplate().trim());
        }
        if (notBlank(request.getProviderExposureTemplate())) {
            settings.setTemplateProviderExposure(request.getProviderExposureTemplate().trim());
        }
        if (request.getCollectionAccounts() != null) {
            List<String> accounts = normalizeAccounts(request.getCollectionAccounts());
            if (accounts.isEmpty()) {
                throw new ApiException("Add at least one Paybill collection account", HttpStatus.BAD_REQUEST);
            }
            settings.setCollectionAccounts(join(accounts));
        }
        if (request.getCollectionNotifyPhones() != null) {
            settings.setCollectionNotifyPhones(join(parsePhones(request.getCollectionNotifyPhones(), true)));
        }
        return repository.save(settings);
    }

    private PlatformSmsSettings seedFromConfig() {
        AppProperties.Templates templates = yamlTemplates();
        PlatformSmsSettings settings = new PlatformSmsSettings();
        settings.setId(PlatformSmsSettings.SINGLETON_ID);
        settings.setEnabled(appProperties.getNotifications().isEnabled());
        settings.setLowBalanceThreshold(lowBalanceThresholdFromYaml());
        settings.setPortalUrl(yamlPortalUrl());
        settings.setTemplateWelcome(templates.getWelcome());
        settings.setTemplateTopup(templates.getTopup());
        settings.setTemplateCollection(templates.getCollection());
        settings.setTemplateLowBalance(templates.getLowBalance());
        settings.setTemplatePlatformTopup(templates.getPlatformTopup());
        settings.setTemplateProviderLow(templates.getProviderLow());
        settings.setTemplateProviderExposure(templates.getProviderExposure());
        settings.setCollectionAccounts(yamlCollectionAccounts());
        settings.setCollectionNotifyPhones(yamlCollectionNotifyPhones());
        return repository.save(settings);
    }

    private AppProperties.Templates yamlTemplates() {
        AppProperties.Templates templates = appProperties.getNotifications().getTemplates();
        return templates != null ? templates : new AppProperties.Templates();
    }

    private String yamlPortalUrl() {
        String url = appProperties.getNotifications().getPortalUrl();
        return firstNonBlank(url, "https://novasms.novastack.co.ke");
    }

    private BigDecimal lowBalanceThresholdFromYaml() {
        BigDecimal yaml = appProperties.getNotifications().getLowBalanceThreshold();
        return yaml != null ? yaml : new BigDecimal("50.00");
    }

    private String yamlCollectionAccounts() {
        List<String> accounts = appProperties.getMpesa().getCollectionAccounts();
        String joined = join(normalizeAccounts(accounts));
        return notBlank(joined) ? joined : DEFAULT_ACCOUNTS;
    }

    private String yamlCollectionNotifyPhones() {
        List<String> phones = appProperties.getMpesa().getCollectionNotifyPhones();
        String joined = join(parsePhones(phones, false));
        return notBlank(joined) ? joined : DEFAULT_PHONES;
    }

    private static List<String> normalizeAccounts(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String value : raw) {
            if (value == null || value.isBlank()) {
                continue;
            }
            for (String part : value.split("[,\\n\\r]+")) {
                String name = part.trim().toUpperCase(Locale.ROOT);
                if (name.isEmpty()) {
                    continue;
                }
                if (!ACCOUNT_NAME.matcher(name).matches()) {
                    throw new ApiException(
                            "Collection account '" + part.trim() + "' must be 2–32 letters (e.g. SHEILA)",
                            HttpStatus.BAD_REQUEST);
                }
                unique.add(name);
            }
        }
        return new ArrayList<>(unique);
    }

    private static List<String> parseAccounts(String stored) {
        try {
            return normalizeAccounts(stored == null ? List.of() : List.of(stored));
        } catch (ApiException ex) {
            return parseAccounts(DEFAULT_ACCOUNTS);
        }
    }

    private List<String> parsePhones(List<String> raw, boolean strict) {
        if (raw == null) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String value : raw) {
            if (value == null || value.isBlank()) {
                continue;
            }
            for (String part : value.split("[,\\n\\r]+")) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String normalized = PhoneNormalizer.normalize(trimmed);
                if (!PhoneNormalizer.isKenyanMobile(normalized)) {
                    if (strict) {
                        throw new ApiException(
                                "Notify phone '" + trimmed + "' is not a valid Kenyan mobile number",
                                HttpStatus.BAD_REQUEST);
                    }
                    continue;
                }
                unique.add(trimmed);
            }
        }
        return new ArrayList<>(unique);
    }

    private List<String> parsePhones(String stored, boolean strict) {
        return parsePhones(stored == null ? List.of() : List.of(stored), strict);
    }

    private static String join(List<String> values) {
        return values == null || values.isEmpty() ? "" : String.join(",", values);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstNonBlank(String value, String fallback) {
        return notBlank(value) ? value.trim() : fallback;
    }
}
