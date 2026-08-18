package com.novastack.sms.domain.enums;

/**
 * How the organization is billed. All models still debit the Nova wallet at send time
 * unless a future policy explicitly allows otherwise.
 */
public enum OrganizationBillingModel {
    /** Customer tops up (M-Pesa). Default SaaS. */
    PREPAID,
    /** Admin allocates credits periodically. */
    MONTHLY,
    /** Internal Novastack app; admin funds the wallet. */
    INTERNAL
}
