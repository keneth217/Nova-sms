# Bulk SMS

Send the same message to many recipients in one request.

```http
POST /api/v1/sms/bulk
```

Permission for scoped API clients: `SMS_BULK`.

## Headers

```http
Content-Type: application/json
Accept: application/json
X-API-Key: nova_live_xxxxxxxxx
Idempotency-Key: announcement-2026-08-18
```

`Idempotency-Key` is optional. See [idempotency](idempotency.md).

## Request body

```json
{
  "recipients": [
    "254712345678",
    "254701234567",
    "254711234567"
  ],
  "message": "Important announcement."
}
```

| Field | Required | Type | Constraints | Description |
| ----- | -------- | ---- | ----------- | ----------- |
| `recipients` | Conditional | string[] | each max 20 | Phone numbers. Provide `recipients`, `groupId`, or both. |
| `message` | Yes | string | max 1600 | SMS body. |
| `senderId` | No | string | max 11 | Optional. Same rules as send SMS. If omitted, TalkSasa uses `TALKSASA_SENDER_ID` (default `TALK-SASA`). |
| `groupId` | Conditional | UUID | — | Nova contact group in **this** organization. All members are included. |

At least one recipient must remain after combining the list and group. Empty result returns HTTP **400** `"No recipients provided"`.

There is **no API maximum recipient count**. Provider HTTP calls are chunked with `SMS_BATCH_SIZE` (default **100**). Message length is capped at 1600 characters.

## Success response

HTTP **200**. Envelope `message` is `"Bulk SMS queued"`.

Delivery runs in the same HTTP request (`processQueuedBatch`). The batch-level `data.status` is always the string `"PROCESSING"`. Per-recipient outcomes are in `data.messages`.

```json
{
  "success": true,
  "message": "Bulk SMS queued",
  "data": {
    "batchId": "b0b0b0b0-1111-2222-3333-444444444444",
    "queuedCount": 3,
    "recipientCount": 3,
    "smsUnits": 3,
    "status": "PROCESSING",
    "messages": [
      {
        "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "messageId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "recipient": "254712345678",
        "status": "ACCEPTED",
        "smsUnits": 1,
        "batchId": "b0b0b0b0-1111-2222-3333-444444444444"
      }
    ]
  }
}
```

| Field | Description |
| ----- | ----------- |
| `batchId` | Nova SMS batch UUID |
| `queuedCount` | Number of messages created |
| `recipientCount` | Same as `queuedCount` |
| `smsUnits` | Total units billed (`units per message × recipient count`) |
| `status` | Always `"PROCESSING"` on this wrapper |
| `messages` | Full `SmsMessageResponse` objects for each recipient |

Wallet debit is per recipient before delivery. Failed recipients are refunded.

## cURL

```bash
curl -X POST "${NOVA_SMS_API_URL}/api/v1/sms/bulk" \
  -H "X-API-Key: ${NOVA_SMS_API_KEY}" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "recipients": [
      "254712345678",
      "254701234567",
      "254711234567"
    ],
    "message": "Important announcement."
  }'
```

## Schedule (related)

```http
POST /api/v1/sms/schedule
```

Same recipient fields as bulk, plus required `scheduledAt` (ISO-8601 instant in the future). Messages are stored as `SCHEDULED` until the dispatcher sends them. Requires `SMS_BULK`.
