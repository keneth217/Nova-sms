# Novastack SMS Gateway

Multi-tenant **Bulk SMS Gateway** built with Spring Boot 3, Java 21, MySQL, Spring Security (JWT + API Key), Spring Data JPA, and Africa's Talking.

## Features

- Multi-tenant organizations with API keys
- SMS wallet with M-Pesa Paybill top-up and transaction history
- Shared platform sender ID + organization-specific sender IDs (pending / approved / rejected)
- Single, bulk, and scheduled SMS delivery
- Africa's Talking provider abstraction with retries and request/response logging
- Delivery report (DLR) callbacks
- Contact groups, contacts, and bulk import
- JWT dashboard auth + API key org access
- Roles: `SUPER_ADMIN`, `ORGANIZATION_ADMIN`
- Dashboard reporting (volume, delivery rate, wallet usage, cost)

## Tech Stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.4 |
| Security | Spring Security, JWT (JJWT), API Key header |
| Persistence | Spring Data JPA, Flyway, MySQL 8 |
| SMS Provider | Africa's Talking (pluggable `SmsProvider`) |
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
├── provider/        # SMS provider abstraction (Africa's Talking)
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

### 2. Configure Africa's Talking

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
| `GET` | `/api/v1/reports/dashboard` | Dashboard metrics |
| `POST` | `/api/v1/dlr/callback` | Delivery report callback |

Additional endpoints:

- `/api/v1/contacts`, `/api/v1/contacts/groups`, `/api/v1/contacts/import`
- `/api/v1/contacts/import/excel` (Excel upload), `/api/v1/contacts/import/excel/template`
- `/api/v1/sender-ids` (+ `PATCH /api/v1/sender-ids/{id}/review` for `SUPER_ADMIN`)
- `/api/v1/wallet/transactions`

### Contacts, groups & Excel import

Each organization manages its own groups and contacts (tenant-isolated).

1. **Create a group**

```http
POST /api/v1/contacts/groups
X-API-Key: nsk_...
Content-Type: application/json

{ "name": "Customers", "description": "Active customers" }
```

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

Poll status:

```http
GET /api/v1/wallet/topup/{transactionId}
Authorization: Bearer <token>
```

**Check transaction** (recommended after STK — polls DB, and if still `PENDING` queries Safaricom then updates DB):

```http
POST /api/v1/wallet/topup/{transactionId}/check
Authorization: Bearer <token>
```

Response fields include `status` (`PENDING` | `COMPLETED` | `FAILED`), `callbackReceived`, `walletCredited`, `mpesaReceipt`.

Frontend tip: after top-up, poll `POST .../check` every 3–5 seconds until `callbackReceived` is `true`.

**Daraja env vars**

| Env | Description |
|-----|-------------|
| `MPESA_SHORTCODE` | Your Paybill number |
| `MPESA_PASSKEY` | Lipa Na M-Pesa Online passkey |
| `MPESA_CONSUMER_KEY` | Daraja app consumer key |
| `MPESA_CONSUMER_SECRET` | Daraja app consumer secret |
| `MPESA_CALLBACK_BASE_URL` | Public HTTPS base URL (e.g. `https://api.yourdomain.com`) |
| `MPESA_BASE_URL` | `https://sandbox.safaricom.co.ke` or `https://api.safaricom.co.ke` |

Register this callback in Daraja / ensure it is reachable:

```
{MPESA_CALLBACK_BASE_URL}/api/v1/mpesa/stk/callback
```

Optional C2B (manual Paybill pay): account number = org `mpesaAccountRef` (returned on registration), confirmation URL:

```
{MPESA_CALLBACK_BASE_URL}/api/v1/mpesa/c2b/confirmation
```

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

Messages are saved as `QUEUED`, then delivered immediately via Africa's Talking (with retries on failure). Status transitions: `QUEUED` → `SENT` → `DELIVERED` / `FAILED`.

### Configure DLR callback

In Africa's Talking, set the delivery report callback to:

```
https://<your-host>/api/v1/dlr/callback
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
   SmsDeliveryService ──► SmsProvider (Africa's Talking)
        │
        ▼
   Status + ProviderRequestLog + Wallet debit/refund
```

### Wallet rules

- SMS cost is per organization (`sms_cost`, default `0.80` KES)
- Balance is checked before sending
- Debit happens at send time; failed sends after max retries are refunded
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
| `AT_USERNAME` / `novastack.africastalking.username` | AT username |
| `AT_API_KEY` / `novastack.africastalking.api-key` | AT API key |
| `AT_BASE_URL` | Sandbox or production AT URL |
| `MPESA_SHORTCODE` / `novastack.mpesa.shortcode` | Paybill shortcode |
| `MPESA_PASSKEY` | STK passkey |
| `MPESA_CONSUMER_KEY` / `MPESA_CONSUMER_SECRET` | Daraja credentials |
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
3. Point `AT_BASE_URL` to production Africa's Talking
4. Put the service behind TLS (reverse proxy / load balancer)
5. Configure AT DLR callback to your public HTTPS URL
6. Monitor Actuator `/actuator/health`
7. Seed a `SUPER_ADMIN` user in the database for sender-ID reviews

## License

Proprietary — Novastack.
