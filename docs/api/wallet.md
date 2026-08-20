# Wallet (external developers)

Show organization SMS credit and accept M-Pesa STK top-ups from **your own site**. Users do not need the Nova SMS organization portal.

```http
GET  /api/v1/wallet/balance
GET  /api/v1/wallet/transactions
POST /api/v1/wallet/topup
GET  /api/v1/wallet/topup/{transactionId}
POST /api/v1/wallet/topup/{transactionId}/check
```

Permissions for scoped API clients:

| Permission | Endpoints |
| ---------- | --------- |
| `WALLET_READ` | `GET /api/v1/wallet/balance`, `GET /api/v1/wallet/transactions` |
| `WALLET_TOPUP` | `POST /api/v1/wallet/topup`, `GET /api/v1/wallet/topup/{id}`, `POST /api/v1/wallet/topup/{id}/check` |

Enable these on the API client (Developer → API Clients → Permissions, or Dashboard → API clients). Existing keys can be updated without rotating.

The wallet is the **organization** wallet. Every API client for that organization shares the same balance.

## Architecture

```text
User on your site
   ↓
Your backend
   ↓
Nova SMS wallet API
   ↓
M-Pesa STK Push
```

Never put the Nova SMS API key in frontend JavaScript. Your UI calls **your** backend; your backend calls Nova SMS.

## Balance

```http
GET /api/v1/wallet/balance
X-API-Key: nova_live_xxxxxxxxx
Accept: application/json
```

```bash
curl -X GET "${NOVA_SMS_API_URL}/api/v1/wallet/balance" \
  -H "X-API-Key: ${NOVA_SMS_API_KEY}" \
  -H "Accept: application/json"
```

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "walletId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "organizationId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "balance": 1500.00,
    "currency": "KES",
    "smsCost": 1.00,
    "availableSms": 1500
  }
}
```

Use `data.balance` and `data.availableSms` on your site.

## Top up (M-Pesa STK)

```http
POST /api/v1/wallet/topup
```

```json
{
  "amount": 500,
  "phoneNumber": "254712345678"
}
```

| Field | Required | Type | Description |
| ----- | -------- | ---- | ----------- |
| `amount` | Yes | number | KES. Minimum `1.00`. |
| `phoneNumber` | Yes | string | M-Pesa phone that receives the STK prompt. Accepts `07…`, `254…`, or `+254…`. |

```bash
curl -X POST "${NOVA_SMS_API_URL}/api/v1/wallet/topup" \
  -H "X-API-Key: ${NOVA_SMS_API_KEY}" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "amount": 500,
    "phoneNumber": "254712345678"
  }'
```

The user enters their M-Pesa PIN on the phone. **Nova SMS is the source of truth** — do not mark the payment successful from the PIN screen.

### Polling rules

1. Read `data.transactionId` from the initial top-up response.
2. Wait approximately 5 seconds before the first status check.
3. Call `POST /api/v1/wallet/topup/{transactionId}/check` every 3–5 seconds.
4. Continue polling while `status` is `PENDING`.
5. `"The transaction is still under processing"` is `PENDING`, not `FAILED`.
6. Stop polling when `status` is `COMPLETED` and `walletCredited=true`.
7. Stop polling when Nova reports a definitive `FAILED` status.
8. Never create another STK Push while the existing transaction is still `PENDING`.
9. `GET /api/v1/wallet/topup/{id}` may be used to recover/read the transaction, but it does not query Safaricom.
10. After successful completion, call `GET /api/v1/wallet/balance` to refresh the organization's SMS balance.

```http
POST /api/v1/wallet/topup/{transactionId}/check
GET  /api/v1/wallet/topup/{transactionId}
```

| Endpoint | Behaviour |
| -------- | --------- |
| `POST …/check` | Required for polling. Reads the database; if still `PENDING`, queries Safaricom and updates the row. |
| `GET …/{id}` | Recover or read the stored row only. Does not call Safaricom. |

```bash
curl -X POST "${NOVA_SMS_API_URL}/api/v1/wallet/topup/${TRANSACTION_ID}/check" \
  -H "X-API-Key: ${NOVA_SMS_API_KEY}" \
  -H "Accept: application/json"
```

Treat the top-up as paid only when `data.status` is `COMPLETED` and `data.walletCredited` is `true`.

| `status` | Meaning | Your app |
| -------- | ------- | -------- |
| `PENDING` | STK sent, or Safaricom still processing (including “The transaction is still under processing”). **Not a failure.** | Keep polling. |
| `COMPLETED` | Payment succeeded. Wallet credited once. | Stop only when `walletCredited` is also `true`. Then `GET /api/v1/wallet/balance`. |
| `FAILED` | User cancelled, insufficient funds, wrong PIN, or similar. | Stop. Show failure. |

| Field | Meaning |
| ----- | ------- |
| `walletCredited` | `true` after Nova SMS credited the organization wallet exactly once. **This is the success flag.** |
| `callbackReceived` | `true` after Safaricom’s STK callback was applied. A successful STK query can credit the wallet before the callback, so this can still be `false` when `walletCredited` is `true`. |
| `mpesaReceipt` | M-Pesa receipt once Safaricom confirms. |
| `resultDesc` | Latest Safaricom description. “Still under processing” means `PENDING`, not `FAILED`. |

Status never moves backwards. `COMPLETED` is never overwritten with `PENDING` or `FAILED`. Keep polling `PENDING`; Nova SMS applies the Safaricom callback even if an earlier check was still processing.

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "transactionId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "checkoutRequestId": "ws_CO_19082026104512345",
    "status": "COMPLETED",
    "amount": 500.00,
    "phoneNumber": "254712345678",
    "mpesaReceipt": "UHJA53YW7O",
    "callbackReceived": true,
    "walletCredited": true
  }
}
```

Initiate response is `PENDING` with `callbackReceived` and `walletCredited` both `false`. A later check may still return `PENDING` with `resultDesc` “The transaction is still under processing” — keep polling.

## Transactions

```http
GET /api/v1/wallet/transactions?page=0&size=20
```

Optional filters: `type=TOPUP|SMS_DEBIT|REFUND|ADJUSTMENT` and `status=PENDING|COMPLETED|FAILED` (repeatable). `data` is a Spring `Page`. Scoped keys can only read their own organization.
