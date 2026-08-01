CREATE TABLE data_bundle_offers (
    id              BINARY(16)     NOT NULL PRIMARY KEY,
    organization_id BINARY(16)     NULL,
    offer_id        VARCHAR(64)    NOT NULL,
    offer_name      VARCHAR(150)   NOT NULL,
    category        VARCHAR(40)    NOT NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    validity        VARCHAR(80)    NULL,
    description     VARCHAR(500)   NULL,
    raw_payload     TEXT           NULL,
    active          TINYINT(1)     NOT NULL DEFAULT 1,
    fetched_at      TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at      TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_dbo_org FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE INDEX idx_dbo_offer_id ON data_bundle_offers (offer_id);
CREATE INDEX idx_dbo_org_category ON data_bundle_offers (organization_id, category);

CREATE TABLE data_bundle_transactions (
    id                   BINARY(16)     NOT NULL PRIMARY KEY,
    organization_id      BINARY(16)     NOT NULL,
    reference            VARCHAR(40)    NOT NULL,
    phone_number         VARCHAR(20)    NOT NULL,
    offer_id             VARCHAR(64)    NOT NULL,
    offer_name           VARCHAR(150)   NOT NULL,
    category             VARCHAR(40)    NULL,
    amount               DECIMAL(12, 2) NOT NULL,
    status               VARCHAR(20)    NOT NULL,
    checkout_request_id  VARCHAR(100)   NULL,
    provider_request_id  VARCHAR(100)   NULL,
    response_code        VARCHAR(40)    NULL,
    response_description VARCHAR(500)   NULL,
    failure_reason       VARCHAR(500)   NULL,
    wallet_debited       TINYINT(1)     NOT NULL DEFAULT 0,
    created_at           TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           TIMESTAMP(6)   NULL,
    CONSTRAINT uk_dbt_reference UNIQUE (reference),
    CONSTRAINT fk_dbt_org FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE INDEX idx_dbt_org_created ON data_bundle_transactions (organization_id, created_at);
CREATE INDEX idx_dbt_phone ON data_bundle_transactions (phone_number);
CREATE INDEX idx_dbt_status ON data_bundle_transactions (status);

CREATE TABLE data_bundle_callback_logs (
    id          BINARY(16)   NOT NULL PRIMARY KEY,
    reference   VARCHAR(40)  NULL,
    payload     TEXT         NOT NULL,
    received_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed   TINYINT(1)   NOT NULL DEFAULT 0,
    process_error VARCHAR(500) NULL
);

CREATE INDEX idx_dbcl_reference ON data_bundle_callback_logs (reference);
CREATE INDEX idx_dbcl_received ON data_bundle_callback_logs (received_at);
