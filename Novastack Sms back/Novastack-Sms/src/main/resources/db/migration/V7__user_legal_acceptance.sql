ALTER TABLE users
    ADD COLUMN terms_accepted TINYINT(1) NOT NULL DEFAULT 0 AFTER token_version,
    ADD COLUMN terms_accepted_at TIMESTAMP(6) NULL AFTER terms_accepted,
    ADD COLUMN privacy_accepted_at TIMESTAMP(6) NULL AFTER terms_accepted_at;
