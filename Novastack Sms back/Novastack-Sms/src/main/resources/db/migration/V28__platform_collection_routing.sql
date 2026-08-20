-- Persist Paybill collection routing and notify phones (YAML remains seed/fallback).
ALTER TABLE platform_sms_settings
    ADD COLUMN collection_accounts VARCHAR(500) NOT NULL DEFAULT 'SHEILA,KENETH',
    ADD COLUMN collection_notify_phones VARCHAR(500) NOT NULL DEFAULT '0711766223,0759728742';
