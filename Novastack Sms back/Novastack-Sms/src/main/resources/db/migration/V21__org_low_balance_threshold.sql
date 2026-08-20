-- Per-organization low-balance SMS alert (KES). Default matches platform config.
ALTER TABLE organizations
    ADD COLUMN low_balance_threshold DECIMAL(14, 4) NOT NULL DEFAULT 50.0000 AFTER sms_cost;
