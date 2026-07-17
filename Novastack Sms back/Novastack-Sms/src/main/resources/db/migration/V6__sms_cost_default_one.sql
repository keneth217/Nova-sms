ALTER TABLE organizations
    ALTER COLUMN sms_cost SET DEFAULT 1.0000;

UPDATE organizations
SET sms_cost = 1.0000
WHERE sms_cost = 0.8000;
