# Idempotency

Scoped API clients can send `Idempotency-Key` on:

```text
POST /api/v1/sms/send
POST /api/v1/sms/bulk
POST /api/v1/sms/batches/{batchId}/resend-failed

POST /api/v1/mpesa/stkpush
POST /api/v1/mpesa/checkout

POST /api/v1/wallet/topup
```

```http
Idempotency-Key: order-123456
```

This is especially important for payments. If Mwalimu sends:

```http
Idempotency-Key: learner-839-payment-2026-08-24
```

and the HTTP client times out and retries, Nova SMS returns the **existing** STK transaction instead of creating another prompt.

Nova stores a hash of the request body with that key **per API client**.

- Same key + same body → original SMS/batch/STK transaction is returned. A second SMS or STK is **not** sent.
- Same key + different body → HTTP **409** `"Idempotency-Key was reused with a different request body"`.
- JWT dashboard users and legacy `nsk_…` keys do not use this store (the header is ignored unless a scoped API client id is present).

`POST /api/v1/sms/{id}/resend` also accepts the header (use a **new** key; reusing the original send key replays the original message).

## Example

```bash
curl -X POST "${NOVA_SMS_API_URL}/api/v1/sms/send" \
  -H "X-API-Key: ${NOVA_SMS_API_KEY}" \
  -H "Idempotency-Key: payment-123456" \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "254712345678",
    "message": "Payment received."
  }'
```

Choose a key that is unique for the business event (payment reference, callback id, receipt number). Reuse that key if the callback or HTTP client retries.

STK example:

```bash
curl -X POST "${NOVA_SMS_API_URL}/api/v1/mpesa/stkpush" \
  -H "X-API-Key: ${NOVA_SMS_API_KEY}" \
  -H "Idempotency-Key: learner-839-payment-2026-08-24" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500,
    "phoneNumber": "254712345678"
  }'
```
