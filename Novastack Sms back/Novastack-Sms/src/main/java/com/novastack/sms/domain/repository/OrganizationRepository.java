package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.enums.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByApiKey(String apiKey);

    Optional<Organization> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    Optional<Organization> findByPhone(String phone);

    Optional<Organization> findByApiKeyAndStatus(String apiKey, OrganizationStatus status);

    Optional<Organization> findByMpesaAccountRefIgnoreCase(String mpesaAccountRef);

    Page<Organization> findByStatus(OrganizationStatus status, Pageable pageable);

    @Query("""
            SELECT o FROM Organization o
            WHERE (:status IS NULL OR o.status = :status)
              AND (
                    :search IS NULL OR :search = ''
                    OR LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR o.phone LIKE CONCAT('%', :search, '%')
                  )
            ORDER BY o.createdAt DESC
            """)
    Page<Organization> search(
            @Param("status") OrganizationStatus status,
            @Param("search") String search,
            Pageable pageable);
}
