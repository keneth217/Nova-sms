package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.ProviderRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProviderRequestLogRepository extends JpaRepository<ProviderRequestLog, UUID> {
}
