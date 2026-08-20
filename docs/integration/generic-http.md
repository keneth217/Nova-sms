# Generic HTTP integration

Any application that can make HTTPS requests can integrate. You do not need an SDK.

## Request

```text
POST {origin}/api/v1/sms/send
X-API-Key: nova_live_xxxxxxxxx
Content-Type: application/json
Accept: application/json
```

```json
{
  "recipient": "254712345678",
  "message": "Hello from Nova SMS"
}
```

Optional: `Idempotency-Key`. Optional `senderId` must be approved for the organization. If omitted, Nova uses the configured TalkSasa default (`TALK-SASA`).

## Response

```json
{
  "success": true,
  "message": "SMS queued",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "messageId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "status": "ACCEPTED",
    "recipient": "254712345678",
    "smsUnits": 1
  }
}
```

Read `data.id` for later `GET /api/v1/sms/{id}/status`.

## Wallet on your site

With `WALLET_READ` / `WALLET_TOPUP` on the API client:

```text
GET  {origin}/api/v1/wallet/balance
POST {origin}/api/v1/wallet/topup
POST {origin}/api/v1/wallet/topup/{transactionId}/check
```

Your users can see SMS credit and top up with M-Pesa without using the Nova SMS portal.

After `POST /topup`, read `data.transactionId`, wait ~5 seconds, then poll `POST …/check` every 3–5 seconds while `PENDING`. Stop when `COMPLETED` and `walletCredited=true`, or on definitive `FAILED`. Then `GET /api/v1/wallet/balance`. See [Wallet](../api/wallet.md).

## Security

```text
NEVER put the Nova SMS API key in frontend JavaScript.
NEVER commit the API key to Git.
NEVER expose the API key to browser users.
Use environment variables or a server-side secrets manager.
For frontend applications, call your own backend,
and let your backend call Nova SMS.
```

Correct:

```text
Browser → Your Backend → Nova SMS API
```

Incorrect:

```text
Browser → Nova SMS API
```
