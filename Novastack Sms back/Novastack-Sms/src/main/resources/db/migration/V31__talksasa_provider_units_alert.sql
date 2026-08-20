ALTER TABLE platform_sms_settings
    ADD COLUMN template_provider_low VARCHAR(1000) NOT NULL
        DEFAULT 'Nova SMS: TalkSasa remaining {units} units. Threshold {threshold}. Top up the provider account.',
    ADD COLUMN talksasa_last_remaining DECIMAL(14, 4) NULL,
    ADD COLUMN talksasa_low_alerted BOOLEAN NOT NULL DEFAULT FALSE;
