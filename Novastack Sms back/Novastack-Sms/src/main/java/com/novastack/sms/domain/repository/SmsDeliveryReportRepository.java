package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.SmsDeliveryReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SmsDeliveryReportRepository extends JpaRepository<SmsDeliveryReport, UUID> {
}
