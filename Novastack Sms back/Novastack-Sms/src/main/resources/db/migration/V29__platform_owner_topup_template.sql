ALTER TABLE platform_sms_settings
    ADD COLUMN template_platform_topup VARCHAR(1000) NOT NULL
        DEFAULT 'Nova SMS: {name} ({account}) wallet credited KES {amount}.{receipt} Balance: KES {balance}.';
