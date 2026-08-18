# Send SMS

Send one SMS through Nova SMS. Your application never talks to the upstream provider.

```http
POST /api/v1/sms/send
```

Permission for scoped API clients: `SMS_SEND`.

## Headers

```http
Content-Type: application/json
Accept: application/json
X-API-Key: nova_live_xxxxxxxxx
Idempotency-Key: payment-123456
```

`Idempotency-Key` is optional. See [idempotency](idempotency.md).

## Request body

```json
{
  "recipient": "254712345678",
  "message": "Your payment has been received."
}
```

Optional approved sender ID:

```json
{
  "recipient": "254712345678",
  "senderId": "CHAMAPLUS",
  "message": "Your payment has been received."
}
```

| Field | Required | Type | Constraints | Description |
| ----- | -------- | ---- | ----------- | ----------- |
| `recipient` | Yes | string | max 20 | Destination phone. Accepts `07…`, `01…`, `254…`, or `+254…`. Stored and returned as `254XXXXXXXXX`. |
| `message` | Yes | string | max 1600 | SMS body. Billed by GSM-7 / Unicode segments. |
| `senderId` | No | string | max 11 | Optional. Must already be **approved** for the organization, or match the TalkSasa default / platform default. If omitted, Nova uses the configured TalkSasa default sender ID (`TALKSASA_SENDER_ID`, currently `TALK-SASA`). |

If `senderId` is not supplied, the configured TalkSasa default sender ID is used. The current default is `TALK-SASA`. Integrators do not call TalkSasa themselves; Nova SMS remains the public API. Change `TALKSASA_SENDER_ID` to use a different default without code changes.

Unapproved `senderId` returns HTTP **400** with `"Sender ID is not approved for this organization"`.

## Success response

HTTP **200**. The envelope `message` is always `"SMS queued"`. Delivery is attempted in the **same HTTP request**, so `data.status` is usually not `PENDING` when you receive the response.

```json
{
  "success": true,
  "message": "SMS queued",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "messageId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "organizationId": "11111111-2222-3333-4444-555555555555",
    "organizationName": "Chamaplus",
    "apiClientId": "99999999-aaaa-bbbb-cccc-ddddeeeeffff",
    "recipient": "254712345678",
    "content": "Your payment has been received.",
    "channel": "SMS",
    "senderId": "CHAMAPLUS",
    "status": "ACCEPTED",
    "cost": 1.00,
    "smsUnits": 1,
    "encoding": "GSM7",
    "characterCount": 32,
    "unitPrice": 1.00,
    "currency": "KES",
    "provider": "talksasa",
    "batchId": null,
    "scheduledAt": null,
    "createdAt": "2026-08-18T09:15:00Z",
    "sentAt": "2026-08-18T09:15:01Z",
    "deliveredAt": null,
    "failureReason": null
  }
}
```

`id` and `messageId` are the same Nova SMS UUID. Use that UUID with status and get endpoints. Do not expect TalkSasa UIDs on this response.

| Field | Description |
| ----- | ----------- |
| `id` | Nova SMS message UUID |
| `messageId` | Same UUID as `id`, as a string |
| `organizationId` | Tenant that was billed |
| `apiClientId` | API client that sent the message, if any |
| `recipient` | Normalized Kenyan mobile (`254…`) |
| `content` | Message text that was sent |
| `channel` | `SMS` (WhatsApp uses `/api/v1/whatsapp/**`, not this endpoint) |
| `senderId` | Sender ID actually used |
| `status` | Nova SMS status after the send attempt. Typical success: `ACCEPTED`, `SENT`, or `DELIVERED`. Failure: `FAILED` or `REJECTED`. |
| `cost` | Wallet debit for this message |
| `smsUnits` | Segment count billed (GSM-7: 160 / 153; Unicode: 70 / 67) |
| `encoding` | `GSM7` or `UCS2` |
| `characterCount` | Character length of `content` |
| `unitPrice` | Price per SMS unit for the organization |
| `currency` | Wallet currency (typically `KES`) |
| `provider` | Internal provider name (`talksasa` by default). Not a TalkSasa response object. |
| `failureReason` | Set when delivery failed |

## cURL

```bash
curl -X POST "${NOVA_SMS_API_URL}/api/v1/sms/send" \
  -H "X-API-Key: ${NOVA_SMS_API_KEY}" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "recipient": "254712345678",
    "message": "Your payment has been received."
  }'
```

With sender ID:

```bash
curl -X POST "${NOVA_SMS_API_URL}/api/v1/sms/send" \
  -H "X-API-Key: ${NOVA_SMS_API_KEY}" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "recipient": "254712345678",
    "senderId": "CHAMAPLUS",
    "message": "Your payment has been received."
  }'
```

Set `NOVA_SMS_API_URL` to the API origin, for example `https://smsapi.novastack.co.ke` or `http://localhost:8092`.
