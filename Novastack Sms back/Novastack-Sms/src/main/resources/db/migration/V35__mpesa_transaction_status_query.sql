-- Internal Daraja Transaction Status queries (Nova-owned fallback when C2B callbacks are delayed).
CREATE TABLE mpesa_transaction_status_query (
    id                          BINARY(16)     NOT NULL PRIMARY KEY,
    mpesa_receipt               VARCHAR(50)    NOT NULL,
    originator_conversation_id  VARCHAR(64)    NULL,
    conversation_id             VARCHAR(64)    NULL,
    status                      VARCHAR(20)    NOT NULL,
    result_code                 VARCHAR(32)    NULL,
    result_desc                 VARCHAR(500)   NULL,
    amount                      DECIMAL(14, 4) NULL,
    bill_ref                    VARCHAR(32)    NULL,
    transaction_status          VARCHAR(40)    NULL,
    raw_result                  VARCHAR(4000)  NULL,
    created_at                  TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_mpesa_txn_status_receipt ON mpesa_transaction_status_query (mpesa_receipt);
CREATE INDEX idx_mpesa_txn_status_originator ON mpesa_transaction_status_query (originator_conversation_id);
