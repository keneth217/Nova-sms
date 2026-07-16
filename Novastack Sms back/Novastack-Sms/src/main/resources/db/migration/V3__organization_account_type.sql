-- Account type for organizations: BUSINESS (ongoing) or EVENT (short-term, e.g. burial notices)
ALTER TABLE organizations
    ADD COLUMN account_type VARCHAR(20) NOT NULL DEFAULT 'BUSINESS' AFTER status,
    ADD COLUMN expires_at TIMESTAMP(6) NULL AFTER account_type;

CREATE INDEX idx_org_account_type ON organizations (account_type);
CREATE INDEX idx_org_expires_at ON organizations (expires_at);
