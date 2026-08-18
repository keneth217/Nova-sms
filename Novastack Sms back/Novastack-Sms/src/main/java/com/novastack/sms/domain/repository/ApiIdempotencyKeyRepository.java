package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.ApiIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApiIdempotencyKeyRepository extends JpaRepository<ApiIdempotencyKey, UUID> {

    Optional<ApiIdempotencyKey> findByApiClientIdAndIdempotencyKey(UUID apiClientId, String idempotencyKey);
}
