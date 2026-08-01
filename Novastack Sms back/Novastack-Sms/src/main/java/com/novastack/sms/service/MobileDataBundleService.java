package com.novastack.sms.service;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.databundle.DataBundleRateLimiter;
import com.novastack.sms.databundle.SafaricomApiErrorMapper;
import com.novastack.sms.databundle.SafaricomDynamicOffersClient;
import com.novastack.sms.domain.entity.DataBundleOffer;
import com.novastack.sms.domain.entity.DataBundleTransaction;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.enums.BundleStatus;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.dto.request.DataBundleOffersRequest;
import com.novastack.sms.dto.request.DataBundlePurchaseRequest;
import com.novastack.sms.dto.response.DataBundleMetricsResponse;
import com.novastack.sms.dto.response.DataBundleOfferResponse;
import com.novastack.sms.dto.response.DataBundleOffersResponse;
import com.novastack.sms.dto.response.DataBundleTransactionResponse;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MobileDataBundleService {

    private static final Pattern FIRST_NUMBER = Pattern.compile("(\\d+)");
    private static final AtomicLong TX_SEQ = new AtomicLong(System.currentTimeMillis() % 1_000_000_000L);

    private final AppProperties appProperties;
    private final SafaricomDynamicOffersClient offersClient;
    private final OfferService offerService;
    private final TransactionService transactionService;
    private final OrganizationRepository organizationRepository;
    private final WalletService walletService;
    private final DataBundleCallbackService callbackService;
    private final DataBundleRateLimiter rateLimiter;

    @Transactional
    public DataBundleOffersResponse fetchOffers(
            UUID organizationId,
            DataBundleOffersRequest request,
            boolean publicCaller) {
        String phone = normalizePhone(request.getPhoneNumber());
        rateLimiter.check(rateLimitKey(organizationId, phone, publicCaller),
                appProperties.getDataBundles().getRateLimitPerMinute());

        // Fetch also validates eligibility per Safaricom Dynamic Offers docs.
        List<DataBundleOfferResponse> offers = offersClient.fetchOffers(phone);
        if (offers.isEmpty()) {
            throw new ApiException(
                    "No data bundle offers are available for this number right now.",
                    HttpStatus.NOT_FOUND);
        }

        if (!publicCaller) {
            offerService.replaceOffers(organizationId, offers);
        }

        return DataBundleOffersResponse.builder()
                .success(true)
                .phoneNumber(phone)
                .offers(offers)
                .build();
    }

    @Transactional
    public DataBundleTransactionResponse purchase(
            UUID organizationId,
            DataBundlePurchaseRequest request,
            boolean publicCaller) {
        String phone = normalizePhone(request.getPhoneNumber());
        rateLimiter.check(rateLimitKey(organizationId, phone, publicCaller),
                appProperties.getDataBundles().getRateLimitPerMinute());
        String reference = transactionService.resolveReference(organizationId, request.getReference());
        String paymentMode = SafaricomDynamicOffersClient.normalizePaymentMode(request.getPaymentMode());
        String paymentPhone = resolvePaymentPhone(phone, paymentMode, request.getPaymentPhoneNumber());

        log.info(
                "Purchase request phone={} offerId={} accountId={} amount={} resource={} mode={}",
                phone,
                request.getOfferId(),
                request.getAccountId(),
                request.getAmount(),
                request.getResourceAmount(),
                paymentMode);

        OfferSnapshot offer = resolveOffer(organizationId, phone, request, publicCaller);
        if (offer.amount() == null || offer.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Invalid offer amount. Fetch offers again and retry.", HttpStatus.BAD_REQUEST);
        }
        if (offer.accountId() == null || offer.accountId().isBlank()) {
            throw new ApiException(
                    "This offer is missing Safaricom account details. Fetch offers again and retry.",
                    HttpStatus.BAD_REQUEST);
        }

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));

        if (!publicCaller) {
            walletService.debitForSms(
                    organizationId,
                    offer.amount(),
                    "BUNDLE-" + reference,
                    "Data bundle " + offer.offerName() + " for " + phone);
        }

        DataBundleTransaction tx = transactionService.createPending(
                org,
                reference,
                phone,
                offer.offerId(),
                offer.offerName(),
                offer.category(),
                offer.amount(),
                !publicCaller);

        String safaricomTxId = nextSafaricomTransactionId();
        tx.setProviderRequestId(safaricomTxId);

        // Bundle recipient stays in msisdn (documented field). Alternate M-Pesa payer is recorded
        // locally; undocumented paymentMsisdn fields are not sent to Safaricom (they cause errors).
        String chargeMsisdn = phone;
        if ("m-pesa".equals(paymentMode) && !paymentPhone.equals(phone)) {
            // STK must target the payer line when paying from another number.
            chargeMsisdn = paymentPhone;
            log.info("M-Pesa purchase: beneficiary={} payer/chargeMsisdn={}", phone, chargeMsisdn);
        }

        try {
            var result = offersClient.purchase(new SafaricomDynamicOffersClient.PurchaseCommand(
                    chargeMsisdn,
                    offer.offerId(),
                    offer.accountId(),
                    offer.amount(),
                    offer.resourceAmount() == null ? "0" : offer.resourceAmount(),
                    offer.validityDays(),
                    safaricomTxId,
                    paymentMode,
                    paymentPhone.equals(phone) ? null : paymentPhone));
            log.info(
                    "Safaricom purchase accepted locally offerId={} accountId={} amount={} resource={} validity={} mode={}",
                    offer.offerId(), offer.accountId(), offer.amount(), offer.resourceAmount(),
                    offer.validityDays(), paymentMode);
            tx.setResponseCode(result.responseCode());
            String description = result.responseDescription();
            if (!paymentPhone.equals(phone)) {
                description = (description == null ? "" : description + " ")
                        + "[Beneficiary " + phone + "; paid from " + paymentPhone + "]";
            }
            tx.setResponseDescription(description == null ? null : description.trim());
            tx.setCheckoutRequestId(result.checkoutRequestId());
            if (result.providerRequestId() != null) {
                tx.setProviderRequestId(result.providerRequestId());
            }

            boolean pending = isPendingPurchaseCode(result.responseCode());
            if (!pending && !SafaricomApiErrorMapper.isSuccess(result.responseCode())) {
                String reason = firstNonBlank(
                        result.responseDescription(),
                        "Bundle purchase was not successful (Safaricom code "
                                + result.responseCode() + ")");
                transactionService.failAndRefund(tx, reason);
                throw new ApiException(reason, HttpStatus.BAD_REQUEST);
            }
            if (pending) {
                // Empty Safaricom body + status pending — keep PENDING for client poll.
                log.info("Purchase left PENDING after Safaricom accept txId={} mode={}",
                        safaricomTxId, paymentMode);
            } else if ("airtime".equals(paymentMode)) {
                // Airtime purchase is synchronous per docs; mark success immediately.
                tx.setStatus(BundleStatus.SUCCESS);
            }
        } catch (ApiException ex) {
            transactionService.failAndRefund(tx, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            transactionService.failAndRefund(tx, ex.getMessage());
            throw SafaricomApiErrorMapper.fromThrowable("purchase", ex);
        }

        return transactionService.toResponse(tx);
    }

    @Transactional(readOnly = true)
    public DataBundleTransactionResponse status(UUID organizationId, String reference, boolean publicCaller) {
        DataBundleTransaction tx;
        if (publicCaller) {
            tx = transactionService.requireByReference(reference);
        } else {
            tx = transactionService.requireForOrganization(organizationId, reference);
        }

        if (tx.getStatus() == BundleStatus.PENDING) {
            String safaricomId = firstNonBlank(tx.getProviderRequestId(), tx.getCheckoutRequestId(), tx.getReference());
            try {
                var remote = offersClient.queryStatus(safaricomId);
                callbackService.applyRemoteStatus(tx, remote.status(), remote.responseCode(), remote.responseDescription());
            } catch (Exception ex) {
                log.warn("Status poll failed for {}: {}", reference, ex.getMessage());
            }
        }
        return transactionService.toResponse(tx);
    }

    @Transactional(readOnly = true)
    public Page<DataBundleTransactionResponse> history(
            UUID organizationId,
            BundleStatus status,
            String phone,
            Instant from,
            Instant to,
            Pageable pageable) {
        return transactionService.history(organizationId, status, phone, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public DataBundleMetricsResponse metrics(UUID organizationId) {
        return transactionService.metrics(organizationId);
    }

    /**
     * Always resolve purchase fields from a fresh Safaricom fetch.
     * Prefer accountId+price+resource fingerprint over client offerId — CVM ids rotate and
     * a stale/wrong offerId must not be paired with another offer's accountId/price.
     */
    private OfferSnapshot resolveOffer(
            UUID organizationId,
            String phone,
            DataBundlePurchaseRequest request,
            boolean publicCaller) {
        String requestedId = request.getOfferId();
        List<DataBundleOfferResponse> live = offersClient.fetchOffers(phone);

        log.info(
                "Purchase resolve requestedOfferId={} fingerprint[accountId={}, amount={}, resource={}] liveOffers={}",
                requestedId,
                request.getAccountId(),
                request.getAmount(),
                request.getResourceAmount(),
                live.stream()
                        .map(o -> o.getOfferName()
                                + "[offeringId=" + o.getOfferId()
                                + ", unique=" + o.getUniqueOfferingId()
                                + ", accountId=" + o.getAccountId()
                                + ", price=" + o.getAmount()
                                + ", resource=" + o.getResourceAmount() + "]")
                        .toList());

        DataBundleOfferResponse matched = null;

        // 1) Fingerprint first when UI sends account/price from the selected card
        matched = matchByFingerprint(live, request);

        // 2) Exact uniqueOfferingId (truly unique per CVM product)
        if (matched == null) {
            matched = live.stream()
                    .filter(o -> requestedId.equals(o.getUniqueOfferingId()))
                    .findFirst()
                    .orElse(null);
        }

        // 3) offeringId — but several products can share the same catalog offeringId
        if (matched == null) {
            List<DataBundleOfferResponse> byOfferingId = live.stream()
                    .filter(o -> requestedId.equals(o.getOfferId()))
                    .toList();
            if (byOfferingId.size() == 1) {
                matched = byOfferingId.get(0);
            } else if (byOfferingId.size() > 1) {
                throw new ApiException(
                        "This offer id matches multiple live products. Refresh offers and try again.",
                        HttpStatus.BAD_REQUEST);
            }
        }

        if (matched == null && !publicCaller) {
            offerService.requireActiveOffer(organizationId, requestedId);
        }

        if (matched == null) {
            throw new ApiException(
                    "Offer not found or expired. Fetch offers again before purchasing.",
                    HttpStatus.BAD_REQUEST);
        }

        if (!requestedId.equals(matched.getOfferId())) {
            log.warn(
                    "Purchase offer remapped requestedOfferId={} → liveOfferingId={} uniqueOfferingId={} name='{}' accountId={} price={}",
                    requestedId,
                    matched.getOfferId(),
                    matched.getUniqueOfferingId(),
                    matched.getOfferName(),
                    matched.getAccountId(),
                    matched.getAmount());
        } else {
            log.info(
                    "Purchase offer locked name='{}' offeringId={} uniqueOfferingId={} accountId={} price={} resource={}",
                    matched.getOfferName(),
                    matched.getOfferId(),
                    matched.getUniqueOfferingId(),
                    matched.getAccountId(),
                    matched.getAmount(),
                    matched.getResourceAmount());
        }

        // Atomic snapshot — never mix fields across offers
        return new OfferSnapshot(
                matched.getOfferId(),
                matched.getOfferName(),
                matched.getCategory(),
                matched.getAmount(),
                matched.getAccountId(),
                matched.getResourceAmount(),
                toValidityDays(matched.getValidity()));
    }

    private static DataBundleOfferResponse matchByFingerprint(
            List<DataBundleOfferResponse> live,
            DataBundlePurchaseRequest request) {
        String accountId = request.getAccountId();
        BigDecimal amount = request.getAmount();
        String resourceAmount = request.getResourceAmount();
        if (accountId == null || accountId.isBlank() || amount == null) {
            return null;
        }
        return live.stream()
                .filter(o -> accountId.equals(o.getAccountId()))
                .filter(o -> o.getAmount() != null && amount.compareTo(o.getAmount()) == 0)
                .filter(o -> resourceAmount == null || resourceAmount.isBlank()
                        || resourceAmount.equals(o.getResourceAmount()))
                .findFirst()
                .orElse(null);
    }

    private UUID rateLimitKey(UUID organizationId, String phone, boolean publicCaller) {
        if (!publicCaller) {
            return organizationId;
        }
        return UUID.nameUUIDFromBytes(("bundle-public:" + phone).getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isPendingPurchaseCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String hay = code.trim().toLowerCase();
        return hay.contains("pending") || hay.contains("processing");
    }

    private String resolvePaymentPhone(String beneficiaryPhone, String paymentMode, String paymentPhoneRaw) {
        if (!"m-pesa".equals(paymentMode) || paymentPhoneRaw == null || paymentPhoneRaw.isBlank()) {
            return beneficiaryPhone;
        }
        return normalizePhone(paymentPhoneRaw);
    }

    private String normalizePhone(String phone) {
        String cleaned = PhoneNormalizer.normalize(phone == null ? "" : phone);
        if (!PhoneNormalizer.isSafaricomMsisdn(cleaned)) {
            throw new ApiException(
                    "Enter a valid Safaricom number (07…, 011…, 2547…, or 25411…).",
                    HttpStatus.BAD_REQUEST);
        }
        return cleaned;
    }

    private static String toValidityDays(String validity) {
        if (validity == null || validity.isBlank()) {
            return "1";
        }
        Matcher matcher = FIRST_NUMBER.matcher(validity);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "1";
    }

    /**
     * Safaricom docs: transactionId is the numeric "x-correlation-conversational value"
     * (sample {@code 12345678901}). Keep numeric — UUIDs are rejected by typed gateways.
     */
    private static String nextSafaricomTransactionId() {
        long seq = Math.floorMod(TX_SEQ.incrementAndGet(), 1000L);
        long base = Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000L);
        if (base < 0) {
            base = -base;
        }
        // 11–12 digit numeric correlation id
        return Long.toString(base + seq);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record OfferSnapshot(
            String offerId,
            String offerName,
            String category,
            BigDecimal amount,
            String accountId,
            String resourceAmount,
            String validityDays) {
    }
}
