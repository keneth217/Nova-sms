# SMS status

Look up or refresh a message using the **Nova SMS UUID** returned as `data.id` / `data.messageId`.

```http
GET /api/v1/sms/{id}
GET /api/v1/sms/{id}/status
```

`{id}` is a UUID, for example `a1b2c3d4-e5f6-7890-abcd-ef1234567890`.

Permission for scoped API clients: `SMS_STATUS`.

`GET /{id}` returns the stored row. `GET /{id}/status` asks the configured provider for an update, then returns the same `SmsMessageResponse` shape.

API clients can only access SMS that belong to **their organization**.

## Example

```http
GET /api/v1/sms/a1b2c3d4-e5f6-7890-abcd-ef1234567890/status
X-API-Key: nova_live_xxxxxxxxx
Accept: application/json
```

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "messageId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "recipient": "254712345678",
    "status": "DELIVERED",
    "smsUnits": 1,
    "failureReason": null,
    "deliveredAt": "2026-08-18T09:16:22Z"
  }
}
```

Unknown id returns HTTP **404** `"SMS message not found"`.

## Status lifecycle

Implemented enum values:

```text
PENDING
QUEUED
PROCESSING
ACCEPTED
SENT
SCHEDULED
DELIVERED
FAILED
REJECTED
CANCELLED
```

Typical send path:

```text
PENDING   (row saved, wallet debited)
   ↓
ACCEPTED / SENT / DELIVERED   (provider accepted the send in the same request)
   ↓
DELIVERED   (after status refresh or sync, when the handset received it)
```

Scheduled messages stay `SCHEDULED` until dispatch.

Failure paths (wallet refunded):

```text
FAILED
REJECTED
CANCELLED
```

`QUEUED` and `PROCESSING` exist on the enum. The current send implementation persists `PENDING`, then maps the provider result. Bulk wrapper `data.status` is the string `"PROCESSING"` and is not this enum.

A scheduler also refreshes in-flight messages (`PENDING`, `QUEUED`, `PROCESSING`, `ACCEPTED`, `SENT`) from the provider.

## cURL

```bash
curl -X GET "${NOVA_SMS_API_URL}/api/v1/sms/${MESSAGE_ID}/status" \
  -H "X-API-Key: ${NOVA_SMS_API_KEY}" \
  -H "Accept: application/json"
```
