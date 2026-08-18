# Idempotency

Scoped API clients can send `Idempotency-Key` on:

- `POST /api/v1/sms/send`
- `POST /api/v1/sms/bulk`

```http
Idempotency-Key: unique-request-id
```

Nova stores a hash of the request body with that key **per API client**.

- Same key + same body → original SMS/batch is returned. A second SMS is **not** sent.
- Same key + different body → HTTP **409** `"Idempotency-Key was reused with a different request body"`.
- JWT dashboard users and legacy `nsk_…` keys do not use this store (the header is ignored unless a scoped API client id is present).

This matters for:

- M-Pesa callbacks
- payment confirmations
- receipts
- OTPs
- school notifications
- transaction notifications

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
