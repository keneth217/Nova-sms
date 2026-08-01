package com.novastack.sms.service;

import com.novastack.sms.domain.entity.DataBundleOffer;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.repository.DataBundleOfferRepository;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.dto.response.DataBundleOfferResponse;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Persists and looks up Safaricom data-bundle offers cached per organization.
 */
@Service
@RequiredArgsConstructor
public class OfferService {

    private final DataBundleOfferRepository offerRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public void replaceOffers(UUID organizationId, List<DataBundleOfferResponse> offers) {
        Organization org = organizationRepository.getReferenceById(organizationId);
        offerRepository.deleteByOrganizationId(organizationId);
        Instant fetchedAt = Instant.now();
        for (DataBundleOfferResponse offer : offers) {
            offerRepository.save(DataBundleOffer.builder()
                    .organization(org)
                    .offerId(offer.getOfferId())
                    .accountId(offer.getAccountId())
                    .offerName(offer.getOfferName())
                    .category(offer.getCategory())
                    .offerSource(offer.getOfferSource())
                    .parentOfferId(offer.getParentOfferId())
                    .amount(offer.getAmount())
                    .resourceAmount(offer.getResourceAmount())
                    .validity(offer.getValidity())
                    .description(offer.getDescription())
                    .active(true)
                    .fetchedAt(fetchedAt)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public DataBundleOffer requireActiveOffer(UUID organizationId, String offerId) {
        return offerRepository
                .findFirstByOrganizationIdAndOfferIdAndActiveTrue(organizationId, offerId)
                .orElseThrow(() -> new ApiException(
                        "Offer not found. Fetch offers again before purchasing.",
                        HttpStatus.BAD_REQUEST));
    }
}
