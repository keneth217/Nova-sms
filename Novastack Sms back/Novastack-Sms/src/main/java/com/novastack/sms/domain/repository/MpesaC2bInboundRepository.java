package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.MpesaC2bInbound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MpesaC2bInboundRepository extends JpaRepository<MpesaC2bInbound, UUID> {

    Optional<MpesaC2bInbound> findByMpesaReceiptIgnoreCase(String mpesaReceipt);
}
