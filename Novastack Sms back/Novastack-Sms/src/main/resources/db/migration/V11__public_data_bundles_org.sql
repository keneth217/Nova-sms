-- System organization for unauthenticated / public Safaricom data-bundle purchases.
INSERT INTO organizations (
    id, name, email, phone, api_key, status, account_type, sms_cost, created_at
) VALUES (
    UNHEX(REPLACE('a0000000-0000-4000-8000-0000000000db', '-', '')),
    'Public Data Bundles',
    'public-bundles@novastack.local',
    '254700000000',
    'public-data-bundles-guest-key-not-for-api',
    'ACTIVE',
    'BUSINESS',
    1.0000,
    CURRENT_TIMESTAMP(6)
);

INSERT INTO wallets (id, organization_id, balance, currency, version, created_at)
VALUES (
    UNHEX(REPLACE('a0000000-0000-4000-8000-0000000000dc', '-', '')),
    UNHEX(REPLACE('a0000000-0000-4000-8000-0000000000db', '-', '')),
    0.0000,
    'KES',
    0,
    CURRENT_TIMESTAMP(6)
);
