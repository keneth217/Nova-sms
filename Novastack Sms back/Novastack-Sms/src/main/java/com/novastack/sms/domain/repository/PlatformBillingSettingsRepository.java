package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.PlatformBillingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformBillingSettingsRepository extends JpaRepository<PlatformBillingSettings, Byte> {
}
