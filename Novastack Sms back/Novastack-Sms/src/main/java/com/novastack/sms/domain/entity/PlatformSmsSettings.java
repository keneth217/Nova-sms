package com.novastack.sms.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "platform_sms_settings")
@Getter
@Setter
@NoArgsConstructor
public class PlatformSmsSettings {

    public static final byte SINGLETON_ID = 1;

    @Id
    private Byte id = SINGLETON_ID;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "low_balance_threshold", nullable = false, precision = 14, scale = 4)
    private BigDecimal lowBalanceThreshold = new BigDecimal("50.00");

    @Column(name = "portal_url", length = 255)
    private String portalUrl = "https://novasms.novastack.co.ke";

    @Column(name = "template_welcome", nullable = false, length = 1000)
    private String templateWelcome;

    @Column(name = "template_topup", nullable = false, length = 1000)
    private String templateTopup;

    @Column(name = "template_collection", nullable = false, length = 1000)
    private String templateCollection;

    @Column(name = "template_low_balance", nullable = false, length = 1000)
    private String templateLowBalance;

    @Column(name = "template_platform_topup", nullable = false, length = 1000)
    private String templatePlatformTopup;

    @Column(name = "template_provider_low", nullable = false, length = 1000)
    private String templateProviderLow;

    @Column(name = "talksasa_last_remaining", precision = 14, scale = 4)
    private BigDecimal talksasaLastRemaining;

    @Column(name = "talksasa_low_alerted", nullable = false)
    private boolean talksasaLowAlerted = false;

    @Column(name = "template_provider_exposure", nullable = false, length = 1000)
    private String templateProviderExposure;

    @Column(name = "talksasa_exposure_alerted", nullable = false)
    private boolean talksasaExposureAlerted = false;

    @Column(name = "collection_accounts", nullable = false, length = 500)
    private String collectionAccounts = "SHEILA,KENETH";

    @Column(name = "collection_notify_phones", nullable = false, length = 500)
    private String collectionNotifyPhones = "0711766223,0759728742";

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
