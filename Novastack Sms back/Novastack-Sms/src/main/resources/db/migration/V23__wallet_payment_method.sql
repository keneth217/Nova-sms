ALTER TABLE wallet_transactions
    ADD COLUMN payment_method VARCHAR(20) NULL AFTER wallet_credited,
    ADD COLUMN bill_ref VARCHAR(32) NULL AFTER payment_method;

UPDATE wallet_transactions
SET payment_method = 'STK_PUSH'
WHERE type = 'TOPUP'
  AND checkout_request_id IS NOT NULL
  AND checkout_request_id <> '';

UPDATE wallet_transactions
SET payment_method = 'PAYBILL'
WHERE type = 'TOPUP'
  AND (checkout_request_id IS NULL OR checkout_request_id = '');
