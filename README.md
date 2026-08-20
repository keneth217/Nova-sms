# Nova SMS

Centralized SMS SaaS Platform & SMS Gateway

Nova SMS is both:

1. A standalone multi-tenant SMS SaaS platform (dashboard, wallets, contacts, sender IDs, reports).
2. An SMS gateway for other applications (Mwalimu Scheme, Chamaplus, Nova POS, SACCO and school systems, other NovaStack apps, and third-party backends).

Integrating applications send SMS through the Nova SMS REST API. They never talk to TalkSasa or any other upstream provider. TalkSasa is the default **internal** delivery provider.

## Main features

- Multi-tenant organizations with prepaid, monthly, or internal wallets
- Single, bulk, and scheduled SMS (and WhatsApp on the same delivery path)
- Contact groups, Excel import, and approved sender IDs
- M-Pesa STK Push wallet funding
- Delivery tracking with Nova SMS statuses (not raw provider payloads)
- Super Admin platform management
- Developer API: hashed `nova_live_…` keys, permissions, rate limits, idempotency
- External apps can show wallet balance and accept M-Pesa top-ups on their own site (`WALLET_READ` / `WALLET_TOPUP`)

## SaaS vs API gateway

SaaS users sign in with email/password (JWT) and use the Vue dashboard. They do **not** need an API key.

API clients belong to an organization. Super Admin (or the org admin) creates a client, copies the key **once**, and the integrating backend sends `X-API-Key`. Sends debit the same organization wallet and use the same `SmsService` as the dashboard.

```text
                   NOVA SMS
                      |
          +-----------+-----------+
          |                       |
      SaaS Users             API Clients
          |                       |
          +-----------+-----------+
                      |
                  SmsService
                      |
              Wallet / billing
                      |
              SMS delivery layer
                      |
                  TalkSasa
```

Future providers can be added without changing `POST /api/v1/sms/send`. See `docs/architecture.md`.

## Repository structure

```text
Nova Sms/
├── Nova sms front/                         Vue 3 frontend
└── Novastack Sms back/
    └── Novastack-Sms/                      Spring Boot backend
```

## Technology

Frontend:

- Vue 3, TypeScript, and Vite
- Pinia for application state
- Vue Router with authentication and role guards
- Axios for API requests
- Tailwind CSS and Heroicons
- Chart.js for dashboard charts
- `xlsx` and ExcelJS for spreadsheets
- jsPDF and AutoTable for PDF reports and invoices

Backend:

- Java 21 and Spring Boot 3.4
- Spring Security with JWT and API-key authentication
- Spring Data JPA, MySQL 8, and Flyway
- TalkSasa (default) or Africa's Talking for SMS delivery
- Safaricom Daraja for M-Pesa STK Push
- Apache POI for Excel contact imports and templates
- Spring scheduler for delayed SMS delivery

## How the system works

### 1. Organization registration

A user creates either a `BUSINESS` or `EVENT` account from the public registration page.

Registration creates all of the following in one transaction:

- An organization with its own tenant ID
- An organization administrator
- A zero-balance prepaid wallet in KES
- An API key for programmatic access
- A unique M-Pesa account reference
- An expiry date for event accounts

Business accounts are intended for ongoing use. Event accounts are active for seven days by default; expired event organizations are blocked by the backend access filter.

### 2. Authentication and route protection

Dashboard users log in with their email address or organization phone number. The backend returns a JWT containing the user's role and organization context.

The frontend separates public and authenticated routes:

- Guests can open the landing, login, and registration pages.
- Guests attempting to open dashboard routes are redirected to login.
- Logged-in users attempting to open public pages are redirected to their dashboard.
- Organization administrators can only open organization features.
- Super administrators can only open authorized platform-management features.

The backend remains the final security boundary. `/api/v1/admin/**` requires `SUPER_ADMIN`; organization data is always resolved from the authenticated tenant.

### 3. Wallet and M-Pesa top-up

Each organization has one prepaid wallet. SMS operations use the organization's configured per-message cost.

The STK Push flow is:

1. The user enters an amount and M-Pesa phone number on the Wallet page.
2. The frontend calls `POST /api/v1/wallet/topup`.
3. The backend creates a pending wallet transaction and requests an STK Push from Daraja.
4. The user enters their M-Pesa PIN.
5. Safaricom calls `/api/v1/mpesa/stk/callback`.
6. The backend verifies the result, marks the transaction completed or failed, and credits the wallet exactly once.
7. The frontend polls the transaction status and updates the balance on the same page.

Manual Paybill payments use the organization's `mpesaAccountRef`. All top-ups and SMS debits appear in wallet transaction history.

### 4. Contacts and groups

Contacts are tenant-isolated and can be created individually, pasted as phone lists, grouped, or imported from Excel. Nova is the source of truth for groups. When TalkSasa sync is enabled, create, rename, and delete also mirror the group on TalkSasa; SMS is still sent from Nova recipient lists. Groups can be renamed or deleted from the Contacts page.

The Excel import format is:

```text
phone       required
firstName   optional
lastName    optional
email       optional
```

Users can download the sample template from Contacts → Import. The backend also exposes:

```http
GET /api/v1/contacts/import/excel/template
```

Contact lists can be exported as styled Excel or PDF reports. Exports respect the current search and group filters.

### 5. Sender IDs

Organizations can request their own branded sender ID. Super administrators review requests (`PENDING` → `APPROVED` / `REJECTED`). An explicit `senderId` on send/bulk must be approved for **that** organization.

When `senderId` is omitted and TalkSasa is the provider, Nova uses `TALKSASA_SENDER_ID` (default `TALK-SASA`). This is configurable and does not require a code change. The product name remains Nova SMS; `TALK-SASA` is only the TalkSasa sender ID.

### 6. Sending SMS

Single and bulk sending support raw phone numbers and saved contact groups.

For an immediate send, the backend:

1. Resolves and de-duplicates recipients.
2. Normalizes Kenyan phone numbers to `254XXXXXXXXX`.
3. Resolves the sender ID (approved org sender, or TalkSasa default `TALK-SASA` when omitted).
4. Checks the organization's access and wallet balance.
5. Debits the wallet.
6. Stores the SMS record.
7. Sends through the configured SMS provider (TalkSasa by default).
8. Records provider requests, responses, retries, and final status.
9. Applies delivery reports and refunds according to the failure path.

Typical message states are:

```text
PENDING → ACCEPTED → SENT → DELIVERED
                           → FAILED / REJECTED
```

### 6b. Sending WhatsApp

WhatsApp uses the same TalkSasa send API with `type=whatsapp`. Nova stores the message, debits the wallet (1 unit per recipient), and schedules locally. Recipients must be able to receive WhatsApp.

```http
POST /api/v1/whatsapp/send
POST /api/v1/whatsapp/bulk
POST /api/v1/whatsapp/schedule
GET  /api/v1/whatsapp/history
```

### 7. Scheduled reminders

Both single and bulk SMS forms allow **Schedule reminder**.

The user selects a future local date and time. The frontend converts it to an ISO timestamp and calls:

```http
POST /api/v1/sms/schedule
```

The backend validates that the time is in the future, debits the wallet, and stores messages with `SCHEDULED` status. `ScheduledSmsDispatcher` checks for due messages every 30 seconds by default and delivers them automatically.

Scheduled messages and their send times are visible in SMS History.

### 8. Delivery reports

TalkSasa delivery is tracked by storing the provider UID and periodically syncing in-flight messages (`GET /sms/{uid}`). Africa's Talking can still post delivery updates to:

```http
POST /api/v1/dlr/callback
```

The backend matches the provider message ID, updates the SMS status, stores delivery timestamps or failure reasons, and exposes the result in history and reports.

### 9. Dashboards and reports

Organization dashboard data comes from live APIs:

- Wallet balance and usage
- SMS sent today and this month
- Delivery and failure counts
- Delivery rate and cost
- Approved sender IDs
- Recent SMS and wallet activity

Super administrators can view organizations, users, sender-ID work, top-up activity, and platform metrics. The frontend does not fall back to mock values when live API calls fail.

## Local setup

### Prerequisites

- Node.js 22.18+ or 24.12+
- npm 10+
- JDK 21+
- MySQL 8+

The Maven wrapper is included, so a global Maven installation is optional.

### 1. Configure MySQL

The backend expects a MySQL database named `novastack_sms`. Review the datasource settings in:

```text
Novastack Sms back/Novastack-Sms/src/main/resources/application.yaml
```

Flyway creates and upgrades the schema automatically at startup. Do not use the example database, JWT, or administrator credentials in production.

### 2. Configure backend integrations

Set the required environment variables before starting the backend:

```env
NOVA_SMS_API_BASE_URL=https://smsapi.novastack.co.ke
SMS_PROVIDER=talksasa
TALKSASA_API_TOKEN=your-talksasa-token
TALKSASA_SENDER_ID=TALK-SASA
TALKSASA_BASE_URL=https://bulksms.talksasa.com/api/v3
TALKSASA_SYNC_CONTACT_GROUPS=true
SMS_BATCH_SIZE=100
SMS_PRICE_PER_UNIT=1.00
SMS_CURRENCY=KES

# Optional fallback provider
# SMS_PROVIDER=africastalking
AT_USERNAME=sandbox
AT_API_KEY=your-africas-talking-api-key
AT_BASE_URL=https://api.sandbox.africastalking.com

MPESA_SHORTCODE=174379
MPESA_PASSKEY=your-daraja-passkey
MPESA_CONSUMER_KEY=your-consumer-key
MPESA_CONSUMER_SECRET=your-consumer-secret
MPESA_CALLBACK_BASE_URL=https://smsapi.novastack.co.ke
MPESA_BASE_URL=https://sandbox.safaricom.co.ke

SUPER_ADMIN_EMAIL=admin@example.com
SUPER_ADMIN_PASSWORD=replace-with-a-strong-password
ORG_EVENT_ACTIVE_DAYS=7

FRONTEND_BASE_URL=http://localhost:5173
MAIL_ENABLED=true
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=your-smtp-user
MAIL_PASSWORD=your-smtp-password
MAIL_SMTP_AUTH=true
MAIL_STARTTLS=true
MAIL_FROM=no-reply@example.com
```

`MPESA_CALLBACK_BASE_URL` must be a publicly reachable HTTPS URL for Daraja callbacks.

### 3. Start the backend

PowerShell:

```powershell
cd "Novastack Sms back\Novastack-Sms"
.\mvnw.cmd spring-boot:run
```

macOS or Linux:

```bash
cd "Novastack Sms back/Novastack-Sms"
./mvnw spring-boot:run
```

Backend URLs:

- API: `http://localhost:8092/api/v1`
- Swagger UI: `http://localhost:8092/swagger-ui.html`
- Health: `http://localhost:8092/actuator/health`

### 4. Configure the frontend

Create `Nova sms front/.env` from `.env.example`:

```env
VITE_API_BASE_URL=http://localhost:8092/api/v1
VITE_USE_MOCK=false
VITE_APP_NAME=Nova SMS
```

During local development, `/api` may also be used because Vite proxies it to port `8092`.

### 5. Start the frontend

```powershell
cd "Nova sms front"
npm install
npm run dev
```

Open `http://localhost:5173`.

Restart the Vite server whenever `.env` changes.

## Authentication methods

### JWT

The web dashboard uses JWT authentication:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "your-password"
}
```

Authenticated dashboard requests send:

```http
Authorization: Bearer <accessToken>
```

### API key (integrations)

Create an API client in Super Admin → Developer → API Clients (or Dashboard → API clients). Send the live key on every request:

```http
X-API-Key: nova_live_xxxxxxxxxxxxxxxxx
```

```bash
export NOVA_SMS_API_KEY="nova_live_xxxxxxxxx"
export NOVA_SMS_API_URL="https://smsapi.novastack.co.ke"
```

The full key is shown only once. Nova stores a SHA-256 hash. Revoked keys stop immediately. Never put the key in frontend JavaScript or Git.

Legacy `nsk_…` organization keys still authenticate as the full organization. New integrations should use `nova_live_…` clients.

### Example send

```bash
curl -X POST "${NOVA_SMS_API_URL}/api/v1/sms/send" \
  -H "X-API-Key: ${NOVA_SMS_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "254712345678",
    "message": "Hello from Nova SMS"
  }'
```

The JSON envelope is `{ "success", "message", "data" }`. `data.id` is a UUID. See `docs/api/send-sms.md`.

For complete API documentation see:

```text
docs/api/
docs/integration/
```

In the running app: Super Admin → Developer, public page `/developers`, and Swagger UI `{origin}/swagger-ui.html`. Production origin is configured with `NOVA_SMS_API_BASE_URL` (default `https://smsapi.novastack.co.ke`).

## Main API areas

- `POST /api/v1/organizations/register` — create an organization, administrator, wallet, and account references
- `POST /api/v1/auth/login` — authenticate a dashboard user
- `GET /api/v1/auth/me` — get the authenticated user's profile
- `POST /api/v1/auth/change-password` — change the authenticated user's password
- `POST /api/v1/auth/forgot-password` — request a one-time password-reset link
- `POST /api/v1/auth/reset-password` — reset a password with the one-time token
- `GET /api/v1/wallet/balance` — get wallet balance and SMS cost
- `POST /api/v1/wallet/topup` — initiate M-Pesa STK Push
- `GET /api/v1/wallet/topup/{transactionId}` — recover/read stored top-up (does not query Safaricom)
- `POST /api/v1/wallet/topup/{transactionId}/check` — poll pending top-up; queries Safaricom while `PENDING`
- `GET /api/v1/wallet/transactions` — list wallet activity
- `POST /api/v1/sms/send` — send one SMS
- `POST /api/v1/sms/bulk` — send to numbers and/or a contact group
- `POST /api/v1/sms/schedule` — schedule messages for later
- `GET /api/v1/sms/history` — list sent and scheduled messages
- `GET /api/v1/sms/{id}` — get one SMS (tenant-scoped)
- `GET /api/v1/sms/{id}/status` — refresh provider status
- `POST /api/v1/whatsapp/send` — send one WhatsApp message
- `POST /api/v1/whatsapp/bulk` — send WhatsApp to numbers and/or a contact group
- `POST /api/v1/whatsapp/schedule` — schedule WhatsApp for later
- `GET /api/v1/whatsapp/history` — list WhatsApp messages
- `GET /api/v1/whatsapp/{id}` — get one WhatsApp message (tenant-scoped)
- `GET /api/v1/whatsapp/{id}/status` — refresh WhatsApp provider status
- `/api/v1/contacts/**` — contacts, groups, imports, and template download
- `/api/v1/api-clients/**` — hashed Nova SMS API keys (create/rotate/revoke)
- `GET /api/v1/admin/api-clients` — platform API clients
- `POST /api/v1/admin/organizations` — create internal/SaaS organizations
- `/api/v1/sender-ids/**` — sender-ID requests and reviews
- `GET /api/v1/reports/dashboard` — organization dashboard metrics
- `/api/v1/admin/**` — super-administrator operations
- `GET /api/v1/admin/talksasa` — TalkSasa platform profile and SMS units

Full request and response schemas are available in Swagger UI.

## Database and startup behavior

Flyway migrations currently cover:

- Initial organizations, users, wallets, SMS, contacts, sender IDs, and provider logs
- M-Pesa Daraja transaction and organization account-reference fields
- Business and event organization account types
- TalkSasa SMS unit, provider, currency, and schedule-owner columns (`V13`)
- TalkSasa contact-group UID on Nova groups (`V14`)
- SMS vs WhatsApp channel on messages (`V15`)
- TalkSasa contact UID per group membership (`V16`)
- API clients, idempotency keys, billing model, SMS client metadata (`V17`)

At startup, the application ensures:

- The platform sender ID exists
- Existing organizations have M-Pesa references
- Existing organizations have wallets
- A super-administrator exists

## Build and verification

Frontend:

```powershell
cd "Nova sms front"
npm run type-check
npm run build
```

Backend:

```powershell
cd "Novastack Sms back\Novastack-Sms"
.\mvnw.cmd clean verify
```

## Security

- Never put a Nova SMS API key in frontend JavaScript.
- Never commit API keys, JWTs, TalkSasa tokens, or M-Pesa secrets to Git.
- Never expose the API key to browser users.
- Scoped live keys can only call `/api/v1/sms/**` and, when granted, `/api/v1/wallet/**`.
- Serve the API over HTTPS in production.

## Production checklist

- Replace all example credentials and secrets.
- Move datasource and JWT configuration to environment-specific secrets.
- Use production TalkSasa and Daraja endpoints.
- Serve the API through HTTPS.
- Set the public M-Pesa and DLR callback URLs.
- Restrict CORS to the deployed frontend origin.
- Back up MySQL and monitor Flyway migrations.
- Monitor `/actuator/health`, provider errors, pending top-ups, and scheduled-message dispatch.
- Build the frontend with `VITE_USE_MOCK=false`.
- Keep API keys, JWTs, M-Pesa credentials, TalkSasa tokens, and other provider credentials out of source control.

## How other applications integrate

1. Super Admin creates (or funds) the organization wallet.
2. Super Admin → Developer → API Clients → create a client for that org.
3. Copy the `nova_live_…` key once and store it as `NOVA_SMS_API_KEY`.
4. From **your backend**, `POST /api/v1/sms/send` with `X-API-Key`.
5. Read `data.id` / `data.status`. Optionally `GET /api/v1/sms/{id}/status`.
6. To show balance and M-Pesa top-up on **your** site, grant `WALLET_READ` and `WALLET_TOPUP`, then call `/api/v1/wallet/balance` and `/api/v1/wallet/topup` from your backend.

Correct architecture:

```text
Browser → Your Backend → Nova SMS API → (internal TalkSasa)
```

## Additional documentation

- Developer docs: `docs/README.md`
- API reference: `docs/api/`
- Integration examples: `docs/integration/`
- Architecture: `docs/architecture.md`
- TalkSasa (internal only): `docs/providers/talksasa.md`
- Frontend notes: `Nova sms front/README.md`
- Backend notes: `Novastack Sms back/Novastack-Sms/README.md`
