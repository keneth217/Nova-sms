-- Survives wallet-credit failures (e.g. column truncation) so receipt recovery
-- can resolve the organization from BillRefNumber instead of trusting the UI.
CREATE TABLE mpesa_c2b_inbound (
    id                     BINARY(16)     NOT NULL PRIMARY KEY,
    mpesa_receipt          VARCHAR(50)    NOT NULL,
    bill_ref               VARCHAR(32)    NULL,
    amount                 DECIMAL(14, 4) NOT NULL,
    phone_number           VARCHAR(64)    NULL,
    mpesa_transaction_date VARCHAR(20)    NULL,
    payload                VARCHAR(4000)  NULL,
    credited               BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_mpesa_c2b_inbound_receipt UNIQUE (mpesa_receipt)
);

CREATE INDEX idx_mpesa_c2b_inbound_credited ON mpesa_c2b_inbound (credited);
