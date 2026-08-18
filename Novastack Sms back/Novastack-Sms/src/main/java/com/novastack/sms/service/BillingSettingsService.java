package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.entity.PlatformBillingSettings;
import com.novastack.sms.domain.repository.PlatformBillingSettingsRepository;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class BillingSettingsService {

    public static final BigDecimal DEFAULT_CUSTOMER_PRICE = new BigDecimal("1.00");
    public static final BigDecimal DEFAULT_PROVIDER_COST = new BigDecimal("0.35");
    public static final String DEFAULT_CURRENCY = "KES";
    private static final BigDecimal FORBIDDEN_PROVIDER_COST = new BigDecimal("0.357");

    private final PlatformBillingSettingsRepository repository;
    private final AppProperties appProperties;

    @Transactional
    public PlatformBillingSettings current() {
        return repository.findById(PlatformBillingSettings.SINGLETON_ID).orElseGet(this::seedFromConfig);
    }

    @Transactional
    public BigDecimal customerPrice() {
        return scale(current().getCustomerPrice(), DEFAULT_CUSTOMER_PRICE);
    }

    @Transactional
    public BigDecimal providerCostPerSms() {
        return scale(current().getProviderCost(), DEFAULT_PROVIDER_COST);
    }

    @Transactional
    public String currency() {
        String value = current().getCurrency();
        return value == null || value.isBlank() ? DEFAULT_CURRENCY : value.trim().toUpperCase();
    }

    @Transactional
    public PlatformBillingSettings update(BigDecimal customerPrice, BigDecimal providerCost, String currency) {
        if (providerCost != null && providerCost.compareTo(FORBIDDEN_PROVIDER_COST) == 0) {
            throw new ApiException("Provider cost must be 0.35, not 0.357", HttpStatus.BAD_REQUEST);
        }
        BigDecimal nextCustomer = scale(customerPrice, DEFAULT_CUSTOMER_PRICE);
        BigDecimal nextProvider = scale(providerCost, DEFAULT_PROVIDER_COST);
        if (nextCustomer.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Customer SMS price must be greater than zero", HttpStatus.BAD_REQUEST);
        }
        if (nextProvider.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException("Provider cost cannot be negative", HttpStatus.BAD_REQUEST);
        }
        PlatformBillingSettings settings = current();
        settings.setCustomerPrice(nextCustomer);
        settings.setProviderCost(nextProvider);
        if (currency != null && !currency.isBlank()) {
            settings.setCurrency(currency.trim().toUpperCase());
        }
        return repository.save(settings);
    }

    private PlatformBillingSettings seedFromConfig() {
        PlatformBillingSettings settings = new PlatformBillingSettings();
        settings.setId(PlatformBillingSettings.SINGLETON_ID);
        settings.setCustomerPrice(configuredCustomerPrice());
        settings.setProviderCost(configuredProviderCost());
        settings.setCurrency(configuredCurrency());
        return repository.save(settings);
    }

    private BigDecimal configuredCustomerPrice() {
        AppProperties.Billing billing = appProperties.getSms().getBilling();
        if (billing != null && billing.getCustomerPrice() != null) {
            return scale(billing.getCustomerPrice(), DEFAULT_CUSTOMER_PRICE);
        }
        AppProperties.Pricing pricing = appProperties.getSms().getPricing();
        if (pricing != null && pricing.getPricePerUnit() != null) {
            return scale(pricing.getPricePerUnit(), DEFAULT_CUSTOMER_PRICE);
        }
        if (appProperties.getSms().getDefaultCost() != null) {
            return scale(appProperties.getSms().getDefaultCost(), DEFAULT_CUSTOMER_PRICE);
        }
        return DEFAULT_CUSTOMER_PRICE;
    }

    private BigDecimal configuredProviderCost() {
        AppProperties.Billing billing = appProperties.getSms().getBilling();
        if (billing != null && billing.getProviderCost() != null) {
            return scale(billing.getProviderCost(), DEFAULT_PROVIDER_COST);
        }
        return DEFAULT_PROVIDER_COST;
    }

    private String configuredCurrency() {
        AppProperties.Billing billing = appProperties.getSms().getBilling();
        if (billing != null && billing.getCurrency() != null && !billing.getCurrency().isBlank()) {
            return billing.getCurrency().trim().toUpperCase();
        }
        AppProperties.Pricing pricing = appProperties.getSms().getPricing();
        if (pricing != null && pricing.getCurrency() != null && !pricing.getCurrency().isBlank()) {
            return pricing.getCurrency().trim().toUpperCase();
        }
        return DEFAULT_CURRENCY;
    }

    static BigDecimal scale(BigDecimal value, BigDecimal fallback) {
        BigDecimal source = value == null ? fallback : value;
        return source.setScale(2, RoundingMode.HALF_UP);
    }
}
