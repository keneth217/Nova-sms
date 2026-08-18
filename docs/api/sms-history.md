# SMS history

List SMS for the authenticated organization.

```http
GET /api/v1/sms/history
```

Permission for scoped API clients: `SMS_HISTORY`.

API clients only see messages that belong to **their organization**. They cannot read other tenants.

## Pagination

Spring Data pagination. Default page size is **20**.

```text
?page=0&size=50
```

| Query | Default | Description |
| ----- | ------- | ----------- |
| `page` | `0` | Zero-based page index |
| `size` | `20` | Page size |
| `sort` | newest first | Optional Spring `sort` parameter |

## Success response

HTTP **200**. `data` is a Spring `Page`.

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "content": [
      {
        "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "messageId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "recipient": "254712345678",
        "content": "Your payment has been received.",
        "status": "DELIVERED",
        "smsUnits": 1,
        "createdAt": "2026-08-18T09:15:00Z"
      }
    ],
    "totalElements": 128,
    "totalPages": 7,
    "size": 20,
    "number": 0,
    "first": true,
    "last": false,
    "empty": false,
    "numberOfElements": 20
  }
}
```

Each `content` item is a full `SmsMessageResponse` (same fields as [send SMS](send-sms.md)).

## cURL

```bash
curl -X GET "${NOVA_SMS_API_URL}/api/v1/sms/history?page=0&size=50" \
  -H "X-API-Key: ${NOVA_SMS_API_KEY}" \
  -H "Accept: application/json"
```
