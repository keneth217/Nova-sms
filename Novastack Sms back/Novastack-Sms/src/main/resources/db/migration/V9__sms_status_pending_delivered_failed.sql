-- Customer-facing SMS statuses: PENDING | DELIVERED | FAILED (+ SCHEDULED for future sends)
UPDATE sms_messages
SET status = 'PENDING'
WHERE status IN ('QUEUED', 'SENT');

UPDATE sms_delivery_reports
SET status = 'PENDING'
WHERE status = 'SENT';
