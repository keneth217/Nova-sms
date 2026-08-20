package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByOrganizationId(UUID organizationId);

    @Query("SELECT SUM(w.balance) FROM Wallet w")
    BigDecimal sumBalances();

    default BigDecimal sumAllBalances() {
        BigDecimal sum = sumBalances();
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.organization.id = :organizationId")
    Optional<Wallet> findByOrganizationIdForUpdate(@Param("organizationId") UUID organizationId);
}
