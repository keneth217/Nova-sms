-- Customer price KSh 1.00 / internal TalkSasa cost KSh 0.35. Wallets are not reset.
ALTER TABLE sms_messages
    ADD COLUMN provider_cost DECIMAL(10, 4) NULL AFTER unit_price,
    ADD COLUMN gross_margin DECIMAL(10, 4) NULL AFTER provider_cost,
    ADD COLUMN billing_status VARCHAR(20) NOT NULL DEFAULT 'CHARGED' AFTER gross_margin;

UPDATE sms_messages
SET provider_cost = ROUND(sms_units * 0.3500, 4),
    gross_margin = ROUND(cost - (sms_units * 0.3500), 4)
WHERE provider_cost IS NULL;

UPDATE sms_messages
SET billing_status = 'CHARGED'
WHERE billing_status IS NULL OR billing_status = '';

UPDATE organizations
SET sms_cost = 1.0000
WHERE sms_cost IS NULL OR sms_cost <> 1.0000;

CREATE TABLE platform_billing_settings (
    id              TINYINT        NOT NULL PRIMARY KEY,
    customer_price  DECIMAL(10, 4) NOT NULL,
    provider_cost   DECIMAL(10, 4) NOT NULL,
    currency        VARCHAR(3)     NOT NULL DEFAULT 'KES',
    updated_at      TIMESTAMP(6)   NULL
);

INSERT INTO platform_billing_settings (id, customer_price, provider_cost, currency, updated_at)
VALUES (1, 1.0000, 0.3500, 'KES', CURRENT_TIMESTAMP(6));
