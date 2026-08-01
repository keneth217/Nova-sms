ALTER TABLE sms_delivery_reports
    MODIFY COLUMN failure_reason VARCHAR(255) NULL,
    ADD COLUMN provider_status VARCHAR(50) NULL AFTER network_code,
    ADD COLUMN phone_number VARCHAR(30) NULL AFTER provider_status;
