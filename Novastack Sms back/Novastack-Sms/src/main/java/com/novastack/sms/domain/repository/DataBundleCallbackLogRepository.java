package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.DataBundleCallbackLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DataBundleCallbackLogRepository extends JpaRepository<DataBundleCallbackLog, UUID> {
}
