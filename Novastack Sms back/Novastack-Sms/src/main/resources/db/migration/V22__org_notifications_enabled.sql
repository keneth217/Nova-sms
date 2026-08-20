-- Per-organization SMS notification preference. Default matches platform config (enabled).
ALTER TABLE organizations
    ADD COLUMN notifications_enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER low_balance_threshold;
