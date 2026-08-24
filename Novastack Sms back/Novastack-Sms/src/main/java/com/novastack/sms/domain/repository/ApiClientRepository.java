package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.ApiClient;
import com.novastack.sms.domain.enums.ApiClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiClientRepository extends JpaRepository<ApiClient, UUID> {

    Optional<ApiClient> findByApiKeyHash(String apiKeyHash);

    Optional<ApiClient> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<ApiClient> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    @Query("SELECT c FROM ApiClient c JOIN FETCH c.organization ORDER BY c.name ASC")
    List<ApiClient> findAllWithOrganization();

    Page<ApiClient> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ApiClient> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    boolean existsByClientCode(String clientCode);

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCaseAndIdNot(UUID organizationId, String name, UUID id);

    long countByStatus(ApiClientStatus status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApiClient c SET c.lastUsedAt = :usedAt WHERE c.id = :id")
    void touchLastUsed(@Param("id") UUID id, @Param("usedAt") Instant usedAt);
}
