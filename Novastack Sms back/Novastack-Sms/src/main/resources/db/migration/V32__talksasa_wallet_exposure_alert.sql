ALTER TABLE platform_sms_settings
    ADD COLUMN template_provider_exposure VARCHAR(1000) NOT NULL
        DEFAULT 'Nova SMS: Org wallets KES {wallets} exceed TalkSasa {units} units. Top up the provider account.',
    ADD COLUMN talksasa_exposure_alerted BOOLEAN NOT NULL DEFAULT FALSE;
