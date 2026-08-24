# Architecture

Nova SMS is one application with two faces:

1. **SaaS** — organizations use the Vue dashboard (JWT) to send SMS, manage contacts, fund wallets, and view history.
2. **Gateway** — other applications use hashed API keys (`X-API-Key`) to send SMS through the same engine.

```text
                   NOVA SMS
                      |
          +-----------+-----------+
          |                       |
      SaaS Users             API Clients
      (JWT dashboard)        (X-API-Key)
          |                       |
          +-----------+-----------+
                      |
                  SmsService
                      |
              Wallet / billing
                      |
              SMS delivery layer
                      |
               Provider adapters
                      |
                  TalkSasa
```

## What integrating applications see

External apps call Nova SMS REST endpoints under `/api/v1/sms/**`. With `WALLET_READ` / `WALLET_TOPUP` they can also call `/api/v1/wallet/**` to show balance and accept M-Pesa top-ups on their own site. They receive Nova SMS message UUIDs, Nova SMS statuses, and Nova SMS error messages.

They do **not** need a TalkSasa account, token, or sender configuration. Nova SMS selects the configured provider and maps provider results into Nova statuses.

## Shared send path

Dashboard sends and API-client sends use the same `SmsService`:

1. Normalize Kenyan mobile numbers to `254XXXXXXXXX`
2. Price the message using GSM/Unicode segment units
3. Resolve an **approved** sender ID for the organization
4. Debit the organization wallet
5. Persist `sms_messages` (tenant-scoped)
6. Deliver through `SmsDeliveryService` → `SmsProvider`
7. Refund on billable failure (`FAILED`, `REJECTED`, `CANCELLED`)

API clients may also send `Idempotency-Key` on SMS send/bulk/resend-failed, STK/checkout, and wallet top-up so retries do not create duplicate SMS or a second STK Push.

## M-Pesa (wallet funding)

Dashboard top-ups and API-client top-ups use the same `WalletService`:

```text
                   NOVA SMS
                      |
          +-----------+-----------+
          |                       |
      SaaS Users             API Clients
      (JWT dashboard)        (X-API-Key)
          |                       |
          +-----------+-----------+
                      |
                 WalletService
                      |
           wallet_transactions
                      |
              MpesaDarajaClient
                      |
                  Safaricom
```

Client apps call `POST /api/v1/mpesa/stkpush` or `POST /api/v1/mpesa/checkout`. They never call Daraja and never receive STK/C2B callbacks. Nova SMS is the callback layer. Status is `GET /api/v1/mpesa/transactions/{id}/status` or `GET /api/v1/mpesa/c2b/transactions`. If a Paybill callback is missing, Nova may ask Daraja Transaction Status internally when the client verifies a receipt. C2B registration is Super Admin only. See [M-Pesa](api/mpesa.md).

## Tenancy

Every SMS row belongs to an organization. Scoped API keys can only read and send for **their** organization. Super Admin JWT can inspect platform-wide SMS.

## Providers

The public API does not change when a provider is added. The default outbound provider is TalkSasa (`SMS_PROVIDER=talksasa`). When a send omits `senderId`, Nova uses `TALKSASA_SENDER_ID` (default `TALK-SASA`). Africa's Talking remains an optional adapter.

See [TalkSasa (internal)](providers/talksasa.md).
