package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.ContactGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactGroupRepository extends JpaRepository<ContactGroup, UUID> {

    List<ContactGroup> findByOrganizationId(UUID organizationId);

    Optional<ContactGroup> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
