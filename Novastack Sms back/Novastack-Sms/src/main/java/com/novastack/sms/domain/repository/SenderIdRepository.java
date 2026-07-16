package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.SenderId;
import com.novastack.sms.domain.enums.SenderIdStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SenderIdRepository extends JpaRepository<SenderId, UUID> {

    List<SenderId> findByOrganizationId(UUID organizationId);

    Optional<SenderId> findByOrganizationIdAndSenderNameIgnoreCase(UUID organizationId, String senderName);

    Optional<SenderId> findFirstByPlatformDefaultTrueAndStatus(SenderIdStatus status);

    boolean existsByOrganizationIdAndSenderNameIgnoreCaseAndStatus(
            UUID organizationId, String senderName, SenderIdStatus status);
    long countByStatus(SenderIdStatus status);
}
