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
@Table(name = "platform_billing_settings")
@Getter
@Setter
@NoArgsConstructor
public class PlatformBillingSettings {

    public static final byte SINGLETON_ID = 1;

    @Id
    private Byte id = SINGLETON_ID;

    @Column(name = "customer_price", nullable = false, precision = 10, scale = 4)
    private BigDecimal customerPrice = new BigDecimal("1.00");

    @Column(name = "provider_cost", nullable = false, precision = 10, scale = 4)
    private BigDecimal providerCost = new BigDecimal("0.35");

    @Column(nullable = false, length = 3)
    private String currency = "KES";

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
