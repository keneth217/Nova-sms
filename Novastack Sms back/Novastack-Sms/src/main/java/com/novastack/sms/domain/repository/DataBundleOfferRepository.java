package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.DataBundleOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataBundleOfferRepository extends JpaRepository<DataBundleOffer, UUID> {

    List<DataBundleOffer> findByOrganizationIdAndActiveTrueOrderByAmountAsc(UUID organizationId);

    Optional<DataBundleOffer> findFirstByOrganizationIdAndOfferIdAndActiveTrue(UUID organizationId, String offerId);

    @Modifying
    @Transactional
    void deleteByOrganizationId(UUID organizationId);
}
