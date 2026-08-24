# Nova SMS API reference

Base path: `{NOVA_SMS_API_BASE_URL}/api/v1`

`NOVA_SMS_API_BASE_URL` is the API origin (no trailing slash). Production default:

```text
https://smsapi.novastack.co.ke
```

All JSON endpoints wrap payloads in:

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

Failures use the same envelope with `"success": false` and no `data`. There is **no** top-level `code` field.

## Authentication

```http
X-API-Key: nova_live_xxxxxxxxxxxxxxxxx
Content-Type: application/json
Accept: application/json
```

See [Authentication](../authentication.md).

## Endpoints for API clients

| Method | Path | Permission | Description |
| ------ | ---- | ---------- | ----------- |
| `POST` | `/api/v1/sms/send` | `SMS_SEND` | Send one SMS |
| `POST` | `/api/v1/sms/bulk` | `SMS_BULK` | Send to many numbers and/or a contact group |
| `POST` | `/api/v1/sms/schedule` | `SMS_BULK` | Schedule SMS for later |
| `GET` | `/api/v1/sms/{id}` | `SMS_STATUS` | Get one message by Nova SMS UUID |
| `GET` | `/api/v1/sms/{id}/status` | `SMS_STATUS` | Refresh delivery status from the provider |
| `GET` | `/api/v1/sms/history` | `SMS_HISTORY` | Paged history for the API client's organization |
| `GET` | `/api/v1/wallet/balance` | `WALLET_READ` | Organization wallet balance and available SMS |
| `GET` | `/api/v1/wallet/transactions` | `WALLET_READ` | Wallet activity for the API client's organization |
| `POST` | `/api/v1/mpesa/stkpush` | `MPESA_STK_PUSH` | Start M-Pesa STK Push (preferred client path) |
| `POST` | `/api/v1/mpesa/checkout` | `MPESA_STK_PUSH` | Lipa Na M-Pesa Online Checkout (same as STK Push) |
| `GET` | `/api/v1/mpesa/transactions/{id}` | `MPESA_STATUS` | Read stored STK transaction |
| `GET` | `/api/v1/mpesa/transactions/{id}/status` | `MPESA_STATUS` | Poll and reconcile a pending STK |
| `GET` | `/api/v1/mpesa/c2b/transactions` | `MPESA_C2B` | List Paybill C2B credits |
| `GET` | `/api/v1/mpesa/c2b/transactions/{id}` | `MPESA_C2B` | Get one C2B credit |
| `POST` | `/api/v1/wallet/topup` | `WALLET_TOPUP` | Same STK initiate as `/mpesa/stkpush` |
| `GET` | `/api/v1/wallet/topup/{id}` | `WALLET_TOPUP` | Read stored top-up status (no Safaricom call) |
| `POST` | `/api/v1/wallet/topup/{id}/check` | `WALLET_TOPUP` | Poll and reconcile a pending STK top-up |
| `POST` | `/api/v1/wallet/topup/verify-receipt` | `WALLET_TOPUP` | Look up a Paybill or STK top-up by M-Pesa receipt |

Scoped `nova_live_…` keys may call `/api/v1/sms/**`, `/api/v1/wallet/**` when granted, and `/api/v1/mpesa/**` client routes when granted. `WALLET_TOPUP` implies `MPESA_STK_PUSH`, `MPESA_STATUS`, and `MPESA_C2B`. `WALLET_READ` implies `MPESA_C2B`. Clients do not configure Safaricom callbacks — Nova handles them; apps poll status or list C2B transactions. Dashboard JWT users and legacy `nsk_…` organization keys are not permission-scoped.

## Guides

- [Send SMS](send-sms.md)
- [Bulk SMS](bulk-sms.md)
- [SMS status](sms-status.md)
- [SMS history](sms-history.md)
- [Wallet](wallet.md)
- [M-Pesa](mpesa.md) — STK / checkout, polling, C2B, Safaricom callbacks on Nova
- [Errors](errors.md)
- [Idempotency](idempotency.md)
- [Rate limits](rate-limits.md)

## OpenAPI

- Swagger UI: `{origin}/swagger-ui.html`
- OpenAPI JSON: `{origin}/v3/api-docs`
