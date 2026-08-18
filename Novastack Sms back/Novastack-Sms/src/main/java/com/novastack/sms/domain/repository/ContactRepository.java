package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, UUID> {

    Page<Contact> findByOrganizationId(UUID organizationId, Pageable pageable);

    Page<Contact> findByOrganizationIdAndGroupsId(UUID organizationId, UUID groupId, Pageable pageable);

    Optional<Contact> findByOrganizationIdAndPhone(UUID organizationId, String phone);

    Optional<Contact> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndPhone(UUID organizationId, String phone);

    boolean existsByOrganizationIdAndPhoneAndIdNot(UUID organizationId, String phone, UUID id);

    List<Contact> findByOrganizationIdAndGroupsId(UUID organizationId, UUID groupId);

    List<Contact> findByOrganizationIdAndIdIn(UUID organizationId, Collection<UUID> ids);

    long countByOrganizationIdAndGroupsId(UUID organizationId, UUID groupId);

    @Query("""
            SELECT DISTINCT c FROM Contact c
            LEFT JOIN FETCH c.groups
            WHERE c.organization.id = :organizationId AND c.id = :id
            """)
    Optional<Contact> findByIdAndOrganizationIdWithGroups(
            @Param("id") UUID id,
            @Param("organizationId") UUID organizationId);
}
