package com.novastack.sms.service;

import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.enums.OrganizationStatus;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationAccessService {

    private final OrganizationRepository organizationRepository;

    @Transactional
    public Organization ensureUsable(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));
        ensureUsable(organization);
        return organization;
    }

    @Transactional
    public void ensureUsable(Organization organization) {
        if (organization == null) {
            return;
        }

        if (organization.isExpired()) {
            if (organization.getStatus() != OrganizationStatus.EXPIRED) {
                organization.setStatus(OrganizationStatus.EXPIRED);
                organizationRepository.save(organization);
            }
            throw new ApiException(
                    "This event account has expired after its 1-week active period. "
                            + "Create a business account for ongoing use, or contact support to extend access.",
                    HttpStatus.FORBIDDEN);
        }

        if (organization.getStatus() == OrganizationStatus.SUSPENDED) {
            throw new ApiException("Organization is suspended", HttpStatus.FORBIDDEN);
        }

        if (organization.getStatus() == OrganizationStatus.PENDING) {
            throw new ApiException("Organization is pending activation", HttpStatus.FORBIDDEN);
        }

        if (organization.getStatus() == OrganizationStatus.EXPIRED) {
            throw new ApiException(
                    "This event account has expired. Create a business account for ongoing SMS use.",
                    HttpStatus.FORBIDDEN);
        }
    }
}
