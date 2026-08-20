-- Persist STK callback vs wallet-credit independently so polling cannot
-- be confused with a final Safaricom callback, and credits stay idempotent.
ALTER TABLE wallet_transactions
    ADD COLUMN mpesa_transaction_date VARCHAR(20) NULL AFTER mpesa_receipt,
    ADD COLUMN callback_received TINYINT(1) NOT NULL DEFAULT 0 AFTER topup_status,
    ADD COLUMN wallet_credited TINYINT(1) NOT NULL DEFAULT 0 AFTER callback_received;

UPDATE wallet_transactions
SET wallet_credited = 1
WHERE type = 'TOPUP'
  AND topup_status = 'COMPLETED';
