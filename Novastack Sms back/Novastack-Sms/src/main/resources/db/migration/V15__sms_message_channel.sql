-- Distinguish SMS vs WhatsApp on the shared message table. Existing rows stay SMS.
ALTER TABLE sms_messages
    ADD COLUMN channel VARCHAR(20) NOT NULL DEFAULT 'SMS' AFTER content;

CREATE INDEX idx_sms_org_channel_created ON sms_messages (organization_id, channel, created_at);
