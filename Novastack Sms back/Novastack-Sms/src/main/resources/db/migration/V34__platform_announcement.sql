CREATE TABLE platform_announcement (
    id              TINYINT         NOT NULL PRIMARY KEY,
    enabled         BOOLEAN         NOT NULL DEFAULT FALSE,
    label           VARCHAR(40)     NOT NULL,
    title           VARCHAR(120)    NOT NULL,
    body            VARCHAR(2000)   NOT NULL,
    tone            VARCHAR(20)     NOT NULL DEFAULT 'INFO',
    updated_at      TIMESTAMP(6)    NULL
);

INSERT INTO platform_announcement (
    id,
    enabled,
    label,
    title,
    body,
    tone,
    updated_at
) VALUES (
    1,
    FALSE,
    'Announcement',
    'Service Notice',
    'Dear Valued Customer, We would like to inform you that we are currently experiencing SMS delivery issues due to a technical issue on the Safaricom network. We apologize for the inconvenience and appreciate your patience and understanding.',
    'INFO',
    CURRENT_TIMESTAMP(6)
);
