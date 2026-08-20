-- C2B confirmation MSISDN is a SHA-256 hash (64 hex chars) on production shortcodes.
-- Keep the phone_number column name so API/UI contracts stay unchanged.
ALTER TABLE wallet_transactions
    MODIFY COLUMN phone_number VARCHAR(64) NULL;
