-- Editable Super Admin SMS notification copy. Seeded from application.yaml defaults.
CREATE TABLE platform_sms_settings (
    id                      TINYINT         NOT NULL PRIMARY KEY,
    enabled                 BOOLEAN         NOT NULL DEFAULT TRUE,
    low_balance_threshold   DECIMAL(14, 4)  NOT NULL,
    portal_url              VARCHAR(255)    NULL,
    template_welcome        VARCHAR(1000)   NOT NULL,
    template_topup          VARCHAR(1000)   NOT NULL,
    template_collection     VARCHAR(1000)   NOT NULL,
    template_low_balance    VARCHAR(1000)   NOT NULL,
    updated_at              TIMESTAMP(6)    NULL
);

INSERT INTO platform_sms_settings (
    id,
    enabled,
    low_balance_threshold,
    portal_url,
    template_welcome,
    template_topup,
    template_collection,
    template_low_balance,
    updated_at
) VALUES (
    1,
    TRUE,
    50.0000,
    'https://novasms.novastack.co.ke',
    'Welcome to Nova SMS, {name}! Your organization account is ready. Top up your wallet via M-Pesa to start sending SMS. {portalUrl}',
    'Nova SMS: KES {amount} credited to your wallet.{receipt} New balance: KES {balance}.',
    'KES {amount} has been received from {payer} for {account}.{receipt}',
    'Nova SMS: Your wallet balance is low (KES {balance}). Top up via M-Pesa to keep sending SMS.',
    CURRENT_TIMESTAMP(6)
);
