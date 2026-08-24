-- Per API-client HTTP request logs. No API keys, bodies, or M-Pesa PINs.
CREATE TABLE api_request_logs (
    id                  BINARY(16)     NOT NULL PRIMARY KEY,
    api_client_id       BINARY(16)     NOT NULL,
    organization_id     BINARY(16)     NOT NULL,
    request_id          VARCHAR(40)    NOT NULL,
    method              VARCHAR(12)    NOT NULL,
    path                VARCHAR(180)   NOT NULL,
    permission          VARCHAR(32)    NULL,
    resource_category   VARCHAR(16)    NOT NULL,
    status              INT            NOT NULL,
    outcome             VARCHAR(20)    NOT NULL,
    duration_ms         INT            NOT NULL,
    ip_address          VARCHAR(64)    NULL,
    user_agent          VARCHAR(180)   NULL,
    created_at          TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_api_req_client_created ON api_request_logs (api_client_id, created_at);
CREATE INDEX idx_api_req_org_created ON api_request_logs (organization_id, created_at);
CREATE INDEX idx_api_req_created ON api_request_logs (created_at);

ALTER TABLE wallet_transactions
    ADD COLUMN api_client_id BINARY(16) NULL AFTER organization_id;

CREATE INDEX idx_wallet_tx_api_client ON wallet_transactions (api_client_id);
