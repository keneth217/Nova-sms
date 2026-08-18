-- TalkSasa provider fields and SMS unit billing. Existing rows are preserved.
ALTER TABLE sms_messages
    ADD COLUMN sms_units INT NOT NULL DEFAULT 1 AFTER cost,
    ADD COLUMN unit_price DECIMAL(10, 4) NULL AFTER sms_units,
    ADD COLUMN currency VARCHAR(3) NULL DEFAULT 'KES' AFTER unit_price,
    ADD COLUMN provider VARCHAR(40) NULL AFTER currency,
    ADD COLUMN schedule_owner VARCHAR(20) NULL AFTER provider;

UPDATE sms_messages
SET unit_price = cost,
    currency = COALESCE(currency, 'KES'),
    provider = COALESCE(provider, 'AFRICAS_TALKING')
WHERE unit_price IS NULL;

UPDATE sms_messages
SET schedule_owner = 'NOVA'
WHERE scheduled_at IS NOT NULL AND schedule_owner IS NULL;

CREATE INDEX idx_sms_status_provider ON sms_messages (status, provider, provider_message_id);
