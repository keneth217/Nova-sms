# Novastack SMS Gateway

Multi-tenant **Bulk SMS Gateway** built with Spring Boot 3, Java 21, MySQL, Spring Security (JWT + API Key), Spring Data JPA, and TalkSasa (default SMS provider).

## Features

- Multi-tenant organizations with API keys
- SMS wallet with M-Pesa Paybill top-up and transaction history
- Shared platform sender ID + organization-specific sender IDs (pending / approved / rejected)
- Single, bulk, and scheduled SMS delivery
- Pluggable SMS providers (`TalkSasaSmsProvider` default, `AfricasTalkingSmsProvider` optional)
- WhatsApp via TalkSasa (`type=whatsapp` on the same send API)
- SMS unit (segment) calculation and wallet billing by units
- TalkSasa status lookup and scheduled status sync
- Delivery report (DLR) callbacks (Africa's Talking)
- Contact groups, contacts, and bulk import (TalkSasa group mirror is best-effort)
- JWT dashboard auth + API key org access
- Roles: `SUPER_ADMIN`, `ORGANIZATION_ADMIN`
- Dashboard reporting (volume, delivery rate, wallet usage, cost, SMS units)

## Tech Stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.4 |
| Security | Spring Security, JWT (JJWT), API Key header |
| Persistence | Spring Data JPA, Flyway, MySQL 8 |
| SMS Provider | TalkSasa (default) via pluggable `SmsProvider` |
| Docs | springdoc-openapi (Swagger UI) |

## Project Structure

```
src/main/java/com/novastack/sms/
├── config/          # Security, OpenAPI, HTTP client, properties
├── controller/      # REST API endpoints
├── domain/
│   ├── entity/      # JPA entities
│   ├── enums/       # Domain enums
│   └── repository/  # Spring Data repositories
├── dto/             # Request/response DTOs
├── exception/       # Global exception handling
├── provider/        # SMS provider abstraction (TalkSasa default, Africa's Talking optional)
├── scheduler/       # Scheduled SMS dispatcher
├── security/        # JWT + API key filters
└── service/         # Business logic
src/main/resources/
├── application.yaml
└── db/migration/    # Flyway SQL migrations
```

## Prerequisites

- JDK 21+
- Maven 3.9+ (or use `./mvnw`)
- MySQL 8 running locally

## Quick Start

### 1. Configure MySQL

Create a database (or let the JDBC URL create it) and set credentials in `src/main/resources/application.yaml`.

### 2. Configure TalkSasa (default SMS provider)

Copy `.env.example` and set:

```bash
export SMS_PROVIDER=talksasa
export TALKSASA_API_TOKEN=your-talksasa-token
export TALKSASA_SENDER_ID=TALK-SASA
export TALKSASA_BASE_URL=https://bulksms.talksasa.com/api/v3
```

Never commit a real token. The frontend must never receive `TALKSASA_API_TOKEN`.

Contact groups and members stay in Nova. When `TALKSASA_SYNC_CONTACT_GROUPS=true` (default) and a token is set, create/rename/delete of groups mirrors TalkSasa `POST/PATCH/DELETE /contacts`. Adding, updating, or removing a grouped contact mirrors TalkSasa `POST /contacts/{group_id}/store`, `PATCH /contacts/{group_id}/update/{uid}`, and `DELETE /contacts/{group_id}/delete/{uid}`. Contacts without a Nova group are not sent to TalkSasa (store requires a group uid). Names are prefixed with the organization name so tenants on one TalkSasa account do not collide. TalkSasa downtime does not fail Nova contact operations. SMS is still sent from Nova recipient lists, not TalkSasa campaigns.

To switch providers without a code change:

```bash
export SMS_PROVIDER=africastalking
export AT_USERNAME=sandbox
export AT_API_KEY=your-africas-talking-api-key
```

### 2b. Configure Africa's Talking (optional fallback)

```bash
export AT_USERNAME=sandbox
export AT_API_KEY=your-africas-talking-api-key
# Production:
# export AT_BASE_URL=https://api.africastalking.com
```

### 3. Run the application

```bash
./mvnw spring-boot:run
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

API base URL: http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui.html

## Authentication

### JWT (dashboard users)

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@acme.com",
  "password": "secret123"
}
```

Use the returned token:

```http
Authorization: Bearer <accessToken>
```

### API Key (organization API access)

Returned on organization registration (`apiKey`). Send on every API call:

```http
X-API-Key: nsk_...
```

## Core API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/auth/login` | Login |
| `POST` | `/api/v1/organizations/register` | Register org + admin |
| `GET` | `/api/v1/wallet/balance` | Wallet balance |
| `POST` | `/api/v1/wallet/topup` | M-Pesa receipt top-up |
| `POST` | `/api/v1/sms/send` | Send single SMS |
| `POST` | `/api/v1/sms/bulk` | Send bulk SMS |
| `POST` | `/api/v1/sms/schedule` | Schedule SMS |
| `GET` | `/api/v1/sms/history` | SMS history |
| `POST` | `/api/v1/whatsapp/send` | Send a WhatsApp message |
| `POST` | `/api/v1/whatsapp/bulk` | Send bulk WhatsApp |
| `POST` | `/api/v1/whatsapp/schedule` | Schedule WhatsApp |
| `GET` | `/api/v1/whatsapp/history` | WhatsApp history |
| `GET` | `/api/v1/admin/overview` | Platform overview counts |
| `GET` | `/api/v1/admin/talksasa` | TalkSasa profile and SMS unit balance |
| `GET` | `/api/v1/reports/dashboard` | Dashboard metrics |
| `POST` | `/api/v1/dlr/callback` | Delivery report callback |

Additional endpoints:

- `/api/v1/contacts`, `/api/v1/contacts/groups`, `/api/v1/contacts/import`
- `/api/v1/contacts/import/excel` (Excel upload), `/api/v1/contacts/import/excel/template`
- `/api/v1/sender-ids` (+ `PATCH /api/v1/sender-ids/{id}/review` for `SUPER_ADMIN`)
- `/api/v1/wallet/transactions`
- `/api/v1/api-clients` (create/rotate/revoke hashed `nova_live_` keys)
- `/api/v1/admin/api-clients`, `POST /api/v1/admin/organizations`, `POST /api/v1/admin/organizations/{id}/wallet/credit`

### Developer API (internal applications)

SaaS customers keep using JWT in the web app. Internal apps (Mwalimu, Chamaplus, Nova POS) send SMS with:

```http
POST /api/v1/sms/send
X-API-Key: nova_live_xxxxxxxxx
Idempotency-Key: payment-123456
Content-Type: application/json

{ "recipient": "254712345678", "message": "Your payment has been received." }
```

Keys are hashed at rest (`api_clients`). Permissions: `SMS_SEND`, `SMS_BULK`, `SMS_STATUS`, `SMS_HISTORY`, `WALLET_READ`, `WALLET_TOPUP`, `MPESA_STK_PUSH`, `MPESA_STATUS`, `MPESA_C2B`. Per-client rate limits return HTTP 429. Both dashboard and API clients call the same `SmsService` and organization wallet. TalkSasa credentials never leave the backend.

Internal apps start M-Pesa STK / Lipa Na M-Pesa Online checkout with:

```http
POST /api/v1/mpesa/stkpush
POST /api/v1/mpesa/checkout
X-API-Key: nova_live_xxxxxxxxx
Idempotency-Key: order-123456
Content-Type: application/json

{ "amount": 500, "phoneNumber": "254712345678" }
```

Poll `GET /api/v1/mpesa/transactions/{transactionId}/status` (or `GET /api/v1/mpesa/checkout/{id}/status`) until `status` is `COMPLETED` and `walletCredited` is `true`. Safaricom callbacks stay on `POST /api/v1/mpesa/stk/callback`. Clients do not receive those posts.

Paybill C2B: `GET /api/v1/mpesa/c2b` for paybill and account, then `POST /api/v1/mpesa/c2b/verify` with the M-Pesa receipt. Safaricom C2B confirmation is posted to this backend, not to the integrating app. Daraja credentials never leave the backend.

### Contacts, groups & Excel import

Each organization manages its own groups and contacts (tenant-isolated).

1. **Create a group**

```http
POST /api/v1/contacts/groups
X-API-Key: nsk_...
Content-Type: application/json

{ "name": "Customers", "description": "Active customers" }
```

Rename or delete a group (tenant-scoped; TalkSasa is mirrored when a provider UID exists):

```http
PATCH /api/v1/contacts/groups/{groupId}
DELETE /api/v1/contacts/groups/{groupId}
```

Update or delete a contact (TalkSasa members are mirrored for each grouped membership):

```http
PATCH /api/v1/contacts/{contactId}
DELETE /api/v1/contacts/{contactId}
```

The API never returns TalkSasa UIDs or tokens. Ungrouped contacts stay on Nova only.

2. **Download Excel template**

```http
GET /api/v1/contacts/import/excel/template
Authorization: Bearer <token>
```

Excel columns: `phone` (required), `firstName`, `lastName`, `email`.

3. **Import Excel into a group** (or omit `groupId` for ungrouped contacts)

```http
POST /api/v1/contacts/import/excel?groupId=<group-uuid>
X-API-Key: nsk_...
Content-Type: multipart/form-data

file=@contacts.xlsx
```

4. **Send SMS**

- To a group only:

```json
{ "groupId": "<group-uuid>", "message": "Hello group" }
```

- To raw numbers (no group):

```json
{ "recipients": ["254712345678", "254700000000"], "message": "Hello" }
```

- Both (merged, de-duplicated):

```json
{
  "groupId": "<group-uuid>",
  "recipients": ["254711111111"],
  "message": "Hello everyone"
}
```

## Example Flows

### Register organization

```http
POST /api/v1/organizations/register
Content-Type: application/json

{
  "name": "Acme Ltd",
  "email": "admin@acme.com",
  "phone": "254712345678",
  "password": "secret123",
  "adminFullName": "Jane Admin"
}
```

Store the returned `apiKey`.

### Top up wallet (M-Pesa Daraja STK Push)

After login, the organization initiates STK Push to your Paybill. The user enters their M-Pesa PIN; Daraja calls your callback and the wallet is credited.

```http
POST /api/v1/wallet/topup
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 500,
  "phoneNumber": "254712345678"
}
```

Poll status with **`POST …/check`** (queries Safaricom when still `PENDING`). `GET …/{id}` only reads the stored row.

```http
POST /api/v1/wallet/topup/{transactionId}/check
Authorization: Bearer <token>
```

Response fields include `status` (`PENDING` | `COMPLETED` | `FAILED`), `callbackReceived`, `walletCredited`, `mpesaReceipt`.

**Polling rules**

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

Do not wait only on `callbackReceived` — a successful STK query can credit the wallet before the Safaricom callback arrives.

**Daraja env vars**

| Env | Description |
|-----|-------------|
| `MPESA_SHORTCODE` | Your Paybill number |
| `MPESA_PASSKEY` | Lipa Na M-Pesa Online passkey |
| `MPESA_CONSUMER_KEY` | Daraja app consumer key |
| `MPESA_CONSUMER_SECRET` | Daraja app consumer secret |
| `MPESA_CALLBACK_BASE_URL` | Public HTTPS base URL (e.g. `https://smsapi.novastack.co.ke`) |
| `MPESA_BASE_URL` | `https://sandbox.safaricom.co.ke` or `https://api.safaricom.co.ke` |
| `MPESA_INITIATOR_NAME` | API operator username (Transaction Status fallback) |
| `MPESA_SECURITY_CREDENTIAL` | Encrypted initiator password (preferred) |
| `MPESA_INITIATOR_PASSWORD` | Plain initiator password if you encrypt at runtime with the Safaricom `.cer` |
| `MPESA_INITIATOR_CERT` | Path to the Safaricom initiator certificate |

Register this callback in Daraja / ensure it is reachable:

```
{MPESA_CALLBACK_BASE_URL}/api/v1/mpesa/stk/callback
```

Optional C2B v2 (Paybill 5687394): account number = org `mpesaAccountRef`. `TransID` in the confirmation is the M-Pesa receipt. Register once with `POST /api/v1/admin/mpesa/c2b/register` (or Daraja C2B v2 `registerurl`). URLs must not contain the word `mpesa`:

```
{MPESA_CALLBACK_BASE_URL}/api/v1/payments/c2b/confirmation
{MPESA_CALLBACK_BASE_URL}/api/v1/payments/c2b/validation
{MPESA_CALLBACK_BASE_URL}/api/v1/payments/transaction-status/result
{MPESA_CALLBACK_BASE_URL}/api/v1/payments/transaction-status/timeout
```

Transaction Status is Nova-internal. If C2B confirmation never arrives, `POST /api/v1/mpesa/c2b/verify` asks Daraja using Nova’s initiator credentials. Clients never call `/mpesa/transactionstatus/v1/query`.

### Send SMS

```http
POST /api/v1/sms/send
X-API-Key: nsk_...
Content-Type: application/json

{
  "recipient": "254712345678",
  "message": "Hello from Novastack",
  "senderId": "NOVASTACK"
}
```

Messages are stored as `PENDING`, submitted to TalkSasa (or the configured provider), then updated to `ACCEPTED` / `SENT` / `DELIVERED` / `FAILED`. Status transitions: `PENDING` → `ACCEPTED` → `SENT` → `DELIVERED` / `FAILED`.

### Check a message by Nova SMS id or TalkSasa uid

```http
GET /api/v1/sms/{id}
Authorization: Bearer <token>
```

`{id}` is the Nova UUID (`data.id`) or TalkSasa uid (`data.providerMessageId`). Lookup is org-scoped: another organization's uid returns 404. Refresh provider status (Nova calls TalkSasa `GET /sms/{uid}` internally):

```http
GET /api/v1/sms/{id}/status
Authorization: Bearer <token>
```

Org list (same as `/history`; not the shared TalkSasa inbox):

```http
GET /api/v1/sms?page=0&size=20
Authorization: Bearer <token>
```

Super Admin live TalkSasa inbox:

```http
GET /api/v1/admin/talksasa/sms?page=1&size=25
GET /api/v1/admin/talksasa/sms/{uid}
```

### WhatsApp (TalkSasa)

WhatsApp uses the same TalkSasa `POST /sms/send` endpoint with `"type":"whatsapp"`. Nova remains the source of truth: wallet, sender IDs, contacts, history, and scheduling stay on Nova. TalkSasa `schedule_time` is not used.

```http
POST /api/v1/whatsapp/send
Authorization: Bearer <token>
Content-Type: application/json

{ "recipient": "0712345678", "message": "Hello on WhatsApp" }
```

Also: `POST /api/v1/whatsapp/bulk`, `POST /api/v1/whatsapp/schedule`, `GET /api/v1/whatsapp/history`, `GET /api/v1/whatsapp/{id}`, `GET /api/v1/whatsapp/{id}/status`.

WhatsApp is always sent through TalkSasa, even if `SMS_PROVIDER=africastalking`. Each WhatsApp message is billed as 1 unit (not GSM segments). Recipients must be able to receive WhatsApp on TalkSasa. The frontend never receives `TALKSASA_API_TOKEN`.

Optional platform WhatsApp unit price (otherwise org `sms_cost` / SMS unit price):

```bash
export NOVASTACK_SMS_PRICING_WHATSAPP_PRICE_PER_UNIT=1.00
```

### TalkSasa profile and SMS units

Super admins can inspect the **platform** TalkSasa account (one token). This is not an organization wallet.

```http
GET /api/v1/admin/talksasa
Authorization: Bearer <super-admin-token>
```

Nova calls TalkSasa `GET /me` and `GET /balance` server-side. The response includes remaining SMS units and a sanitized profile (name, email, status). It never includes `TALKSASA_API_TOKEN`. Customer billing still uses Nova wallets.

### TalkSasa contacts

Nova contacts and groups remain the source of truth. When a contact is in a Nova group that has a TalkSasa group UID, Nova mirrors:

- `POST /contacts/{group_id}/store` on create / assign / import
- `PATCH /contacts/{group_id}/update/{uid}` on contact update
- `DELETE /contacts/{group_id}/delete/{uid}` on remove-from-group or delete contact

TalkSasa contact UIDs are stored per membership (`contact_provider_uids`) and are never returned to the frontend. A contact in two Nova groups is stored twice on TalkSasa (once per group).

### How to test SMS

1. Set `TALKSASA_API_TOKEN` and `TALKSASA_SENDER_ID`.
2. Top up an organization wallet.
3. Send via `POST /api/v1/sms/send` with recipient `0712345678` (normalized to `254712345678`).
4. Confirm `providerMessageId` is the TalkSasa UID, `smsUnits` matches message length, and the wallet was debited.
5. Poll `GET /api/v1/sms/{id}/status` or wait for the status-sync job.

### Rotate TalkSasa credentials

1. Generate a new API token in the TalkSasa portal.
2. Set `TALKSASA_API_TOKEN` on the server and restart the API.
3. Revoke the old token in TalkSasa.
4. Confirm a test SMS still sends. Do not put the token in Git, logs, or the Vue app.

### Configure DLR callback (Africa's Talking fallback)

In Africa's Talking, set the delivery report callback to:

```
https://smsapi.novastack.co.ke/api/v1/dlr/callback
```

## Architecture

```
Client (JWT / API Key)
        │
        ▼
   REST Controllers
        │
        ▼
   Domain Services ──► MySQL (Flyway)
        │
        ▼
   SmsDeliveryService ──► SmsProvider (TalkSasa default)
        │
        ▼
   Status + ProviderRequestLog + Wallet debit/refund
```

### Wallet rules

- SMS cost billed to the customer is per organization (`sms_cost`, default from `novastack.sms.pricing.price-per-unit`); TalkSasa's own cost stays with the platform
- Balance is checked before sending, using SMS units (GSM-7 / Unicode segments), not 1 message = 1 SMS
- Debit happens at send time; failed sends that were not accepted by the provider are refunded
- Optimistic/pessimistic locking on wallet updates (`@Version` + `PESSIMISTIC_WRITE`)

### Sender IDs

- Platform default sender (`NOVASTACK`) is seeded on startup
- Orgs request custom sender IDs (`PENDING` until `SUPER_ADMIN` approves)
- Send validates ownership / platform default before delivery

## Configuration Reference

| Property / Env | Description |
|----------------|-------------|
| `spring.datasource.*` | MySQL connection |
| `novastack.jwt.secret` | JWT signing secret |
| `SMS_PROVIDER` / `novastack.sms.provider` | `talksasa` (default) or `africastalking` |
| `TALKSASA_API_TOKEN` / `novastack.sms.talksasa.api-token` | TalkSasa Bearer token (never expose to the frontend) |
| `TALKSASA_SENDER_ID` / `novastack.sms.talksasa.default-sender-id` | TalkSasa sender when `senderId` is omitted (default `TALK-SASA`, max 11 characters). Override without a code change. |
| `TALKSASA_BASE_URL` | TalkSasa API v3 base URL |
| `TALKSASA_SYNC_CONTACT_GROUPS` / `novastack.sms.talksasa.sync-contact-groups` | Mirror Nova contact groups and members to TalkSasa (default `true`) |
| `NOVASTACK_SMS_PRICING_WHATSAPP_PRICE_PER_UNIT` | Optional WhatsApp unit price; otherwise org SMS cost is used |
| `SMS_BATCH_SIZE` / `novastack.sms.batch-size` | Recipients per TalkSasa send request |
| `SMS_PRICE_PER_UNIT` / `novastack.sms.pricing.price-per-unit` | Default customer price per SMS unit |
| `AT_USERNAME` / `novastack.africastalking.username` | AT username (fallback provider) |
| `AT_API_KEY` / `novastack.africastalking.api-key` | AT API key |
| `AT_BASE_URL` | Sandbox or production AT URL |
| `MPESA_SHORTCODE` / `novastack.mpesa.shortcode` | Paybill shortcode |
| `MPESA_PASSKEY` | STK passkey |
| `MPESA_CONSUMER_KEY` / `MPESA_CONSUMER_SECRET` | Daraja credentials |
| `MPESA_INITIATOR_NAME` / `MPESA_SECURITY_CREDENTIAL` | Transaction Status (internal C2B fallback) |
| `MPESA_CALLBACK_BASE_URL` | Public HTTPS URL for callbacks |
| `novastack.sms.default-cost` | Default SMS unit cost |
| `novastack.sms.max-retries` | Provider retry attempts |

Optional per-organization AT credentials: set `at_username` / `at_api_key` on the `organizations` row.

## Building & Testing

```bash
./mvnw clean verify
```

Tests use an in-memory H2 profile (`src/test/resources/application-test.yaml`).

## Production Notes

1. Change `novastack.jwt.secret` to a strong secret (256-bit+)
2. Use strong DB credentials; do not commit secrets
3. Point TalkSasa credentials at your production token. Use `SMS_PROVIDER=africastalking` only if you intentionally switch providers.
4. Put the service behind TLS (reverse proxy / load balancer)
5. Configure AT DLR callback to your public HTTPS URL
6. Monitor Actuator `/actuator/health`
7. Seed a `SUPER_ADMIN` user in the database for sender-ID reviews

## License

Proprietary — Novastack.
