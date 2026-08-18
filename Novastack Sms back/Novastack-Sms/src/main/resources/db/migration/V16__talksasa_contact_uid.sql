-- TalkSasa contact UIDs are scoped to a group. Nova contacts remain the source of truth.
CREATE TABLE contact_provider_uids (
    contact_id           BINARY(16)   NOT NULL,
    group_id             BINARY(16)   NOT NULL,
    provider_contact_uid VARCHAR(64)  NOT NULL,
    created_at           TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (contact_id, group_id),
    CONSTRAINT fk_cpu_contact FOREIGN KEY (contact_id) REFERENCES contacts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cpu_group FOREIGN KEY (group_id) REFERENCES contact_groups (id) ON DELETE CASCADE,
    CONSTRAINT uk_cpu_provider_uid UNIQUE (provider_contact_uid)
);
