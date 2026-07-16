-- V1__init_schema.sql
CREATE TABLE organizations (
    id              BINARY(16)     NOT NULL PRIMARY KEY,
    name            VARCHAR(150)   NOT NULL,
    email           VARCHAR(180)   NOT NULL,
    phone           VARCHAR(30)    NOT NULL,
    api_key         VARCHAR(64)    NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    sms_cost        DECIMAL(10, 4) NOT NULL DEFAULT 0.8000,
    at_username     VARCHAR(100)   NULL,
    at_api_key      VARCHAR(255)   NULL,
    created_at      TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)   NULL,
    CONSTRAINT uk_org_email UNIQUE (email),
    CONSTRAINT uk_org_api_key UNIQUE (api_key)
);

CREATE TABLE users (
    id              BINARY(16)     NOT NULL PRIMARY KEY,
    email           VARCHAR(180)   NOT NULL,
    password        VARCHAR(100)   NOT NULL,
    full_name       VARCHAR(150)   NOT NULL,
    role            VARCHAR(30)    NOT NULL,
    organization_id BINARY(16)     NULL,
    enabled         TINYINT(1)     NOT NULL DEFAULT 1,
    created_at      TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)   NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_org FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE TABLE wallets (
    id              BINARY(16)     NOT NULL PRIMARY KEY,
    organization_id BINARY(16)     NOT NULL,
    balance         DECIMAL(14, 4) NOT NULL DEFAULT 0.0000,
    currency        VARCHAR(3)     NOT NULL DEFAULT 'KES',
    version         BIGINT         NULL,
    created_at      TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)   NULL,
    CONSTRAINT uk_wallet_org UNIQUE (organization_id),
    CONSTRAINT fk_wallet_org FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE TABLE wallet_transactions (
    id              BINARY(16)     NOT NULL PRIMARY KEY,
    organization_id BINARY(16)     NOT NULL,
    wallet_id       BINARY(16)     NOT NULL,
    type            VARCHAR(20)    NOT NULL,
    amount          DECIMAL(14, 4) NOT NULL,
    balance_before  DECIMAL(14, 4) NOT NULL,
    balance_after   DECIMAL(14, 4) NOT NULL,
    reference       VARCHAR(64)    NOT NULL,
    description     VARCHAR(255)   NULL,
    mpesa_receipt   VARCHAR(50)    NULL,
    topup_status    VARCHAR(20)    NULL,
    created_at      TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_wallet_tx_ref UNIQUE (reference),
    CONSTRAINT fk_wallet_tx_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_wallet_tx_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id)
);

CREATE INDEX idx_wallet_tx_org ON wallet_transactions (organization_id);

CREATE TABLE sender_ids (
    id                   BINARY(16)   NOT NULL PRIMARY KEY,
    organization_id      BINARY(16)   NULL,
    sender_name          VARCHAR(11)  NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    is_platform_default  TINYINT(1)   NOT NULL DEFAULT 0,
    reason               VARCHAR(255) NULL,
    created_at           TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           TIMESTAMP(6) NULL,
    CONSTRAINT fk_sender_org FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE UNIQUE INDEX idx_sender_org_name ON sender_ids (organization_id, sender_name);

CREATE TABLE sms_messages (
    id                  BINARY(16)     NOT NULL PRIMARY KEY,
    organization_id     BINARY(16)     NOT NULL,
    recipient           VARCHAR(20)    NOT NULL,
    content             VARCHAR(1600)  NOT NULL,
    sender_id           VARCHAR(11)    NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    cost                DECIMAL(10, 4) NOT NULL,
    provider_message_id VARCHAR(100)   NULL,
    batch_id            BINARY(16)     NULL,
    scheduled_at        TIMESTAMP(6)   NULL,
    sent_at             TIMESTAMP(6)   NULL,
    delivered_at        TIMESTAMP(6)   NULL,
    failure_reason      VARCHAR(500)   NULL,
    retry_count         INT            NOT NULL DEFAULT 0,
    created_at          TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)   NULL,
    CONSTRAINT fk_sms_org FOREIGN KEY (organization_id) REFERENCES organizations (id)
);

CREATE INDEX idx_sms_org_created ON sms_messages (organization_id, created_at);
CREATE INDEX idx_sms_status ON sms_messages (status);
CREATE INDEX idx_sms_provider_id ON sms_messages (provider_message_id);
CREATE INDEX idx_sms_scheduled ON sms_messages (scheduled_at, status);

CREATE TABLE sms_delivery_reports (
    id                  BINARY(16)   NOT NULL PRIMARY KEY,
    sms_message_id      BINARY(16)   NOT NULL,
    provider_message_id VARCHAR(100) NULL,
    network_code        VARCHAR(20)  NULL,
    failure_reason      VARCHAR(20)  NULL,
    status              VARCHAR(20)  NOT NULL,
    raw_payload         TEXT         NULL,
    received_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_dlr_sms FOREIGN KEY (sms_message_id) REFERENCES sms_messages (id)
);

CREATE INDEX idx_dlr_message ON sms_delivery_reports (sms_message_id);
CREATE INDEX idx_dlr_provider_id ON sms_delivery_reports (provider_message_id);

CREATE TABLE contact_groups (
    id              BINARY(16)   NOT NULL PRIMARY KEY,
    organization_id BINARY(16)   NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(255) NULL,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6) NULL,
    CONSTRAINT fk_group_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uk_group_org_name UNIQUE (organization_id, name)
);

CREATE TABLE contacts (
    id              BINARY(16)   NOT NULL PRIMARY KEY,
    organization_id BINARY(16)   NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    first_name      VARCHAR(80)  NULL,
    last_name       VARCHAR(80)  NULL,
    email           VARCHAR(180) NULL,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6) NULL,
    CONSTRAINT fk_contact_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uk_contact_org_phone UNIQUE (organization_id, phone)
);

CREATE TABLE contact_group_members (
    contact_id BINARY(16) NOT NULL,
    group_id   BINARY(16) NOT NULL,
    PRIMARY KEY (contact_id, group_id),
    CONSTRAINT fk_cgm_contact FOREIGN KEY (contact_id) REFERENCES contacts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cgm_group FOREIGN KEY (group_id) REFERENCES contact_groups (id) ON DELETE CASCADE
);

CREATE TABLE provider_request_logs (
    id               BINARY(16)   NOT NULL PRIMARY KEY,
    sms_message_id   BINARY(16)   NULL,
    provider         VARCHAR(50)  NOT NULL,
    request_payload  TEXT         NULL,
    response_payload TEXT         NULL,
    http_status      INT          NULL,
    success          TINYINT(1)   NOT NULL DEFAULT 0,
    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
