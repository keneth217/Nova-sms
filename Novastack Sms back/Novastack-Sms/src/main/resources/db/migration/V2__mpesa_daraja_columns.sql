-- Add M-Pesa Daraja STK tracking columns
ALTER TABLE wallet_transactions
    ADD COLUMN phone_number VARCHAR(20) NULL AFTER mpesa_receipt,
    ADD COLUMN checkout_request_id VARCHAR(64) NULL AFTER phone_number,
    ADD COLUMN merchant_request_id VARCHAR(64) NULL AFTER checkout_request_id,
    ADD COLUMN result_code VARCHAR(20) NULL AFTER merchant_request_id,
    ADD COLUMN result_desc VARCHAR(255) NULL AFTER result_code;

CREATE UNIQUE INDEX uk_wallet_tx_checkout ON wallet_transactions (checkout_request_id);

ALTER TABLE organizations
    ADD COLUMN mpesa_account_ref VARCHAR(12) NULL AFTER api_key;

CREATE UNIQUE INDEX uk_org_mpesa_account_ref ON organizations (mpesa_account_ref);
