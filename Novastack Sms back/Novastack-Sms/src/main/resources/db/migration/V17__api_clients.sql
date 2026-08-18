-- API clients (hashed nova_live_ keys), permissions, idempotency, billing model, SMS client metadata.
-- Existing organizations.api_key (nsk_…) remains for legacy SaaS keys.

ALTER TABLE organizations
    ADD COLUMN billing_model VARCHAR(20) NOT NULL DEFAULT 'PREPAID' AFTER account_type;

CREATE TABLE api_clients (
    id                     BINARY(16)   NOT NULL PRIMARY KEY,
    organization_id        BINARY(16)   NOT NULL,
    name                   VARCHAR(120) NOT NULL,
    client_code            VARCHAR(64)  NOT NULL,
    api_key_hash           CHAR(64)     NOT NULL,
    api_key_prefix         VARCHAR(24)  NOT NULL,
    status                 VARCHAR(20)  NOT NULL,
    rate_limit_per_minute  INT          NOT NULL DEFAULT 100,
    last_used_at           TIMESTAMP(6) NULL,
    expires_at             TIMESTAMP(6) NULL,
    created_at             TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             TIMESTAMP(6) NULL,
    CONSTRAINT fk_api_client_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uk_api_clients_code UNIQUE (client_code),
    CONSTRAINT uk_api_clients_hash UNIQUE (api_key_hash),
    CONSTRAINT uk_api_clients_org_name UNIQUE (organization_id, name)
);

CREATE INDEX idx_api_clients_org ON api_clients (organization_id);
CREATE INDEX idx_api_clients_prefix ON api_clients (api_key_prefix);

CREATE TABLE api_client_permissions (
    api_client_id BINARY(16)  NOT NULL,
    permission    VARCHAR(32) NOT NULL,
    PRIMARY KEY (api_client_id, permission),
    CONSTRAINT fk_api_perm_client FOREIGN KEY (api_client_id) REFERENCES api_clients (id) ON DELETE CASCADE
);

CREATE TABLE api_idempotency_keys (
    id              BINARY(16)   NOT NULL PRIMARY KEY,
    api_client_id   BINARY(16)   NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash    CHAR(64)     NOT NULL,
    resource_type   VARCHAR(20)  NOT NULL,
    resource_id     BINARY(16)   NOT NULL,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_idem_client FOREIGN KEY (api_client_id) REFERENCES api_clients (id) ON DELETE CASCADE,
    CONSTRAINT uk_idem_client_key UNIQUE (api_client_id, idempotency_key)
);

ALTER TABLE sms_messages
    ADD COLUMN api_client_id BINARY(16) NULL AFTER organization_id,
    ADD COLUMN encoding VARCHAR(10) NULL AFTER sms_units,
    ADD COLUMN character_count INT NULL AFTER encoding;

ALTER TABLE sms_messages
    ADD CONSTRAINT fk_sms_api_client FOREIGN KEY (api_client_id) REFERENCES api_clients (id);
