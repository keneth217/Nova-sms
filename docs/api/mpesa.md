# M-Pesa API

Clients use the same M-Pesa REST API that the Nova SMS dashboard uses. There is no separate client-only M-Pesa gateway, and there is no `novapay.novastack.co.ke` API in this product.

The API is exposed through the public base URL:

```text
https://smsapi.novastack.co.ke
```

Nova SMS uses M-Pesa to **fund the organization SMS wallet** (STK Push and Paybill C2B). It is not a general payment-collection API: clients cannot pass their own `accountReference` / `transactionDesc`, there is no B2C payout, and there is no `PaymentIntent` resource.

## Callbacks (Nova handles Safaricom)

**Clients do not configure or implement Safaricom callbacks. Nova SMS handles Safaricom STK and C2B callbacks internally. Client applications initiate payments through the Nova SMS API and retrieve transaction status through the authenticated API.**

That is the main reason to integrate M-Pesa through Nova SMS instead of Daraja. Client applications do **not** need to understand:

- Daraja callback structure
- Safaricom credentials
- STK callback parsing
- C2B validation
- M-Pesa receipt extraction
- callback security
- transaction reconciliation

They only integrate with Nova SMS (`X-API-Key`, JSON, poll or list transactions).

C2B URL registration with Daraja (`POST /api/v1/admin/mpesa/c2b/register`) is **Super Admin / internal**. Ordinary API clients never receive that endpoint.

A later Nova webhook (`payment.success`, `payment.failed`, `c2b.received`) may be added for production apps. **Polling remains the supported fallback** today: `GET /api/v1/mpesa/transactions/{id}/status` and `GET /api/v1/mpesa/c2b/transactions`.

## Client-facing endpoints

Primary map for integrating apps (`MpesaController`):

| Method | Path | Permission |
| ------ | ---- | ---------- |
| `POST` | `/api/v1/mpesa/stkpush` | `MPESA_STK_PUSH` |
| `POST` | `/api/v1/mpesa/checkout` | `MPESA_STK_PUSH` |
| `GET` | `/api/v1/mpesa/transactions/{id}` | `MPESA_STATUS` |
| `GET` | `/api/v1/mpesa/transactions/{id}/status` | `MPESA_STATUS` |
| `GET` | `/api/v1/mpesa/c2b/transactions` | `MPESA_C2B` |
| `GET` | `/api/v1/mpesa/c2b/transactions/{id}` | `MPESA_C2B` |

Also available (same permissions):

| Method | Path | Permission | Purpose |
| ------ | ---- | ---------- | ------- |
| `GET` | `/api/v1/mpesa/checkout/{id}` | `MPESA_STATUS` | Alias of `GET /mpesa/transactions/{id}` |
| `GET` | `/api/v1/mpesa/checkout/{id}/status` | `MPESA_STATUS` | Alias of `GET /mpesa/transactions/{id}/status` |
| `GET` | `/api/v1/mpesa/c2b` | `MPESA_C2B` | Paybill and organization account to show the customer |
| `POST` | `/api/v1/mpesa/c2b/verify` | `MPESA_C2B` | Look up a credit by M-Pesa receipt |

`WALLET_TOPUP` still grants STK, checkout, and status. `MPESA_STK_PUSH` also grants status. `WALLET_READ` or `WALLET_TOPUP` grants C2B.

Wallet aliases (same `WalletService`, same transactions):

| Method | Path | Permission | Purpose |
| ------ | ---- | ---------- | ------- |
| `POST` | `/api/v1/wallet/topup` | `WALLET_TOPUP` | Same STK initiate as `/mpesa/stkpush` |
| `GET` | `/api/v1/wallet/topup/{id}` | `WALLET_TOPUP` | Same as `GET /mpesa/transactions/{id}` |
| `POST` | `/api/v1/wallet/topup/{id}/check` | `WALLET_TOPUP` | Same as `GET /mpesa/transactions/{id}/status` |
| `POST` | `/api/v1/wallet/topup/verify-receipt` | `WALLET_TOPUP` | Look up a Paybill or STK top-up by M-Pesa receipt |
| `GET` | `/api/v1/wallet/transactions` | `WALLET_READ` | Wallet transaction history |
| `GET` | `/api/v1/wallet/balance` | `WALLET_READ` | Organization wallet balance |

Balance and history field details: [Wallet](wallet.md).

## Client authentication

Like SMS, M-Pesa client routes are **not** public.

Clients authenticate using:

1. **Portal / JWT** — `Authorization: Bearer <token>` from the Nova SMS dashboard.
2. **API key** — used by external applications such as Mwalimu Scheme, Chamaplus, POS, and other backends.

External applications send:

```http
X-API-Key: nova_live_xxxxxxxxx
Content-Type: application/json
Accept: application/json
```

The API key identifies the organization. **Do not send `organizationId`.** The backend takes the organization from the authenticated principal (`SecurityUtils.requireOrganizationId()`), the same way the SMS API does.

Scoped `nova_live_…` keys need `MPESA_STK_PUSH` / `MPESA_STATUS`, or `WALLET_TOPUP` (which implies both). C2B needs `MPESA_C2B`, or `WALLET_READ` / `WALLET_TOPUP`.

| Permission | Allows |
| ---------- | ------ |
| `MPESA_STK_PUSH` | Initiate STK at `POST /api/v1/mpesa/stkpush` or `POST /api/v1/mpesa/checkout`. Also allows status. |
| `MPESA_STATUS` | Get and refresh STK / checkout transactions. Does not allow initiate. |
| `MPESA_C2B` | `GET /api/v1/mpesa/c2b/transactions`, `GET /api/v1/mpesa/c2b/transactions/{id}`, plus instructions `GET /c2b` and `POST /c2b/verify`. Not Daraja registration. |
| `WALLET_TOPUP` | Legacy wallet STK paths, STK/checkout routes, and C2B. |
| `WALLET_READ` | Wallet balance, transaction history, and C2B instructions/verify. |

- Missing permission → HTTP **403** (`API key is missing permission MPESA_STK_PUSH`)
- Rate limit exceeded → HTTP **429**
- Invalid or missing API key → HTTP **401**

Dashboard JWT users and legacy `nsk_…` organization keys are not permission-scoped.

## STK Push

```http
POST /api/v1/mpesa/stkpush
X-API-Key: nova_live_xxxxxxxxx
Idempotency-Key: order-123456
Content-Type: application/json
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
| `phoneNumber` | Yes | string | M-Pesa phone that receives the STK prompt. Accepts `07…`, `254…`, or `+254…`. Stored as `254XXXXXXXXX`. |

Clients do **not** send `accountReference` or `transactionDesc`. Nova uses the organization's Paybill account reference (`mpesaAccountRef`, for example `NOVAC727`) and the platform Paybill shortcode.

`POST /api/v1/mpesa/checkout` is the same Lipa Na M-Pesa Online request (same body, same `Idempotency-Key` hash, same `WalletService`). Poll either `GET /api/v1/mpesa/transactions/{id}/status` or `GET /api/v1/mpesa/checkout/{id}/status`.

```bash
curl -X POST "${NOVA_SMS_API_URL}/api/v1/mpesa/stkpush" \
  -H "X-API-Key: ${NOVA_SMS_API_KEY}" \
  -H "Idempotency-Key: order-123456" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "amount": 500,
    "phoneNumber": "254712345678"
  }'
```

The backend then:

1. Authenticates the API key (or JWT).
2. Identifies the organization.
3. Validates amount and Kenyan M-Pesa phone.
4. Creates a `wallet_transactions` row (`type=TOPUP`, `status=PENDING`).
5. Sends Lipa Na M-Pesa STK through Safaricom Daraja to the platform Paybill.
6. Stores `checkoutRequestId` / `merchantRequestId`.
7. Returns the Nova transaction id. The wallet is **not** credited yet.

```json
{
  "success": true,
  "message": "STK Push sent. Enter M-Pesa PIN on your phone.",
  "data": {
    "transactionId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "checkoutRequestId": "ws_CO_19082026104512345",
    "merchantRequestId": "29115-34620561-1",
    "customerMessage": "Success. Request accepted for processing",
    "status": "PENDING",
    "amount": 500.00,
    "phoneNumber": "254712345678",
    "callbackReceived": false,
    "walletCredited": false
  }
}
```

The client's application must **not** treat the STK HTTP response as payment success. The customer still has to enter their M-Pesa PIN.

Send `Idempotency-Key` on STK. Same key + same body returns the original transaction (no second STK). Same key + different body returns HTTP **409**. JWT dashboard users and legacy `nsk_…` keys ignore the header.

## Payment status

Poll Nova SMS, not Safaricom:

```http
GET /api/v1/mpesa/transactions/{transactionId}/status
X-API-Key: nova_live_xxxxxxxxx
```

```bash
curl -X GET "${NOVA_SMS_API_URL}/api/v1/mpesa/transactions/${TRANSACTION_ID}/status" \
  -H "X-API-Key: ${NOVA_SMS_API_KEY}" \
  -H "Accept: application/json"
```

| Endpoint | Behaviour |
| -------- | --------- |
| `GET /api/v1/mpesa/transactions/{id}/status` | Required for polling. Reads MySQL; if still `PENDING`, queries Daraja STK query and updates the row. |
| `GET /api/v1/mpesa/transactions/{id}` | Recover or read the stored row only. Does **not** call Safaricom. |
| `GET /api/v1/mpesa/checkout/{id}/status` | Same refresh as `GET …/transactions/{id}/status`. |
| `GET /api/v1/mpesa/checkout/{id}` | Same stored-row read as `GET …/transactions/{id}`. |
| `POST /api/v1/wallet/topup/{id}/check` | Same refresh as `GET …/status` (wallet alias). |

Statuses (`TopupStatus`):

| `status` | Meaning | Your app |
| -------- | ------- | -------- |
| `PENDING` | STK sent, or Safaricom still processing (including “The transaction is still under processing”). **Not a failure.** | Keep polling. |
| `COMPLETED` | Payment succeeded. Wallet credited once. | Stop only when `walletCredited` is also `true`. Then `GET /api/v1/wallet/balance`. |
| `FAILED` | User cancelled, insufficient funds, wrong PIN, timeout, or similar. There is no separate `CANCELLED` status. | Stop. Show failure. |

Treat the top-up as paid only when `data.status` is `COMPLETED` and `data.walletCredited` is `true`.

| Field | Meaning |
| ----- | ------- |
| `transactionId` | Nova SMS wallet transaction UUID. Use this in poll URLs. |
| `checkoutRequestId` | Safaricom STK id. Stored for callbacks; you do not need to send it back. |
| `walletCredited` | `true` after Nova credited the organization wallet **exactly once**. This is the success flag. |
| `callbackReceived` | `true` after Safaricom’s STK callback was applied. A successful STK query can credit the wallet before the callback, so this can still be `false` when `walletCredited` is `true`. |
| `mpesaReceipt` | M-Pesa receipt once Safaricom confirms. |
| `resultDesc` | Latest Safaricom description. “Still under processing” means `PENDING`, not `FAILED`. |

Status never moves backwards. `COMPLETED` is never overwritten with `PENDING` or `FAILED`.

Polling rules:

1. Read `data.transactionId` from the initiate response.
2. Wait about 5 seconds before the first check.
3. Call `GET /api/v1/mpesa/transactions/{id}/status` every 3–5 seconds while `PENDING`.
4. Stop when `COMPLETED` and `walletCredited=true`, or on definitive `FAILED`.
5. Retry the initiate with the same `Idempotency-Key` if the HTTP client timed out. Do not send a new key while the first transaction is still `PENDING`.

```text
Client
   │
   │ POST /api/v1/mpesa/stkpush
   ▼
Nova SMS API
   │
   │ Daraja STK Push
   ▼
Safaricom
   │
   │ STK prompt
   ▼
Customer
   │
   │ M-Pesa PIN
   ▼
Safaricom
   │
   │ POST /api/v1/mpesa/stk/callback
   ▼
Nova SMS API
   │
   ▼
wallet_transactions = COMPLETED / FAILED
wallet credited once on success
```

## Safaricom callbacks (Nova backend, not yours)

**Clients do not configure or implement Safaricom callbacks.** Nova SMS handles STK and C2B callbacks internally. Client applications initiate payments through this API and retrieve status with an authenticated `X-API-Key`.

How clients complete a payment:

| Flow | What you call | What you never call |
| ---- | ------------- | ------------------- |
| STK / checkout | `GET /api/v1/mpesa/transactions/{id}/status` | `POST /api/v1/mpesa/stk/callback` |
| Paybill C2B | `GET /api/v1/mpesa/c2b/transactions` or `GET /api/v1/mpesa/c2b/transactions/{id}` | `POST /api/v1/payments/c2b/confirmation` and `POST /api/v1/payments/transaction-status/result` |

Safaricom-only paths (`permitAll`):

| Method | Path | Who calls it |
| ------ | ---- | ------------ |
| `POST` | `/api/v1/mpesa/stk/callback` | Safaricom |
| `POST` | `/api/v1/payments/c2b/confirmation` | Safaricom |
| `POST` | `/api/v1/payments/c2b/validation` | Safaricom |
| `POST` | `/api/v1/payments/transaction-status/result` | Safaricom (Transaction Status result) |
| `POST` | `/api/v1/payments/transaction-status/timeout` | Safaricom (Transaction Status timeout) |

Nova also calls Daraja **Transaction Status** (`/mpesa/transactionstatus/v1/query`) internally when `POST /api/v1/mpesa/c2b/verify` does not find the receipt yet. That is a secondary reconciliation path if the C2B callback never arrived. Clients never send initiator credentials, never set ResultURL, and never call Daraja. Credit still uses Safaricom’s `BillRefNumber`, not the verifying organization.

## Paybill C2B

Customers pay the **platform Paybill** using the organization’s account reference (for example `NOVAC727`). There is no client `POST` that starts C2B. Nova credits the wallet from the Safaricom confirmation `BillRefNumber`.

1. Optional: `GET /api/v1/mpesa/c2b` — show paybill and account to the customer.
2. Customer pays from M-Pesa.
3. Safaricom posts confirmation to **Nova** (not to your app).
4. Your app lists or gets the credit: `GET /api/v1/mpesa/c2b/transactions` and `GET /api/v1/mpesa/c2b/transactions/{id}`.
5. Optional: `POST /api/v1/mpesa/c2b/verify` with the receipt from the M-Pesa SMS. If Nova has no callback yet and Transaction Status is configured, Nova asks Safaricom and returns `source=SAFARICOM_QUERY`. Verify again a few seconds later.

Treat C2B as paid when `walletCredited` is `true` (list/get) or when verify returns `found` and `walletCredited`. Do not send `organizationId`.

Registering C2B URLs with Daraja is **Super Admin only** (not an API-key client):

```http
GET  /api/v1/admin/mpesa/c2b/urls
POST /api/v1/admin/mpesa/c2b/register
```

## What is not exposed

| Assumed / other-product path | In Nova SMS |
| ---------------------------- | ----------- |
| `https://novapay.novastack.co.ke` | Not this API. Origin is `https://smsapi.novastack.co.ke`. |
| Client Daraja callback URL | Not supported. Callbacks terminate on Nova SMS. |
| C2B register for API keys | Super Admin only. |
| `POST /api/v1/mpesa/b2c` | **Not implemented.** |
| Client `accountReference` / `transactionDesc` | Set by Nova from the organization Paybill account. |
| `CANCELLED` status | Mapped to `FAILED`. |

## Architecture

Each application gets its own `nova_live_…` key. Nova SMS is the only party that talks to Safaricom.

```text
                         ┌──────────────────┐
                         │   Mwalimu Scheme │
                         └────────┬─────────┘
                                  │
                         X-API-Key│
                                  ▼
┌──────────────┐        ┌────────────────────┐
│   Chamaplus  │───────▶│                    │
└──────────────┘        │      NOVA SMS      │
                        │                    │
┌──────────────┐        │  SMS API           │
│     POS      │───────▶│  M-Pesa API        │
└──────────────┘        │  Wallet API        │
                        │  API Clients       │
                        └─────────┬──────────┘
                                  │
                         Nova-managed
                         credentials
                                  │
                                  ▼
                         ┌────────────────┐
                         │   Safaricom    │
                         │    Daraja      │
                         └───────┬────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
                 STK Push                    C2B
                    │                         │
                    ▼                         ▼
                Customer                  Paybill
                    │                         │
                    └────────────┬────────────┘
                                 │
                              Callback
                                 │
                                 ▼
                         ┌────────────────┐
                         │   NOVA SMS     │
                         │ callback layer │
                         └────────────────┘
```

Dashboard JWT users and API-key clients call the same `WalletService`. TalkSasa is unrelated to this flow.
