-- Stats-only Paybill deposits (SHEILA / KENETH, etc). Never credits an org wallet.
CREATE TABLE paybill_collections (
    id                   BINARY(16)     NOT NULL PRIMARY KEY,
    bill_ref             VARCHAR(32)    NOT NULL,
    amount               DECIMAL(14, 4) NOT NULL,
    mpesa_receipt        VARCHAR(50)    NOT NULL,
    phone_number         VARCHAR(64)    NULL,
    mpesa_transaction_date VARCHAR(20)  NULL,
    payer_name           VARCHAR(120)   NULL,
    created_at           TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_paybill_collection_receipt UNIQUE (mpesa_receipt)
);

CREATE INDEX idx_paybill_collection_bill_ref ON paybill_collections (bill_ref);
CREATE INDEX idx_paybill_collection_created ON paybill_collections (created_at);
