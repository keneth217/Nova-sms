-- Store TalkSasa contact-group UID. Nova groups remain the source of truth.
ALTER TABLE contact_groups
    ADD COLUMN provider_group_uid VARCHAR(64) NULL AFTER description;

CREATE UNIQUE INDEX uk_contact_groups_provider_uid ON contact_groups (provider_group_uid);
