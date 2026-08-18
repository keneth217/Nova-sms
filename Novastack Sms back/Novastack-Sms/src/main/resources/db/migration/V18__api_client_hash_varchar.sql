ALTER TABLE api_clients
    MODIFY COLUMN api_key_hash VARCHAR(64) NOT NULL;

ALTER TABLE api_idempotency_keys
    MODIFY COLUMN request_hash VARCHAR(64) NOT NULL;
