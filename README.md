# Nova SMS

Nova SMS is a multi-tenant messaging platform for businesses, event organizers, and other groups that need to send single, bulk, or scheduled SMS messages. It combines a Vue dashboard with a Spring Boot API, prepaid organization wallets, M-Pesa funding, contact management, sender IDs, delivery tracking, and administrative reporting.

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
- Africa's Talking for SMS delivery
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

Contacts are tenant-isolated and can be created individually, pasted as phone lists, grouped, or imported from Excel.

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

Organizations can use the shared platform sender ID or request their own branded sender ID.

Custom sender IDs move through:

```text
PENDING → APPROVED
        → REJECTED
```

Only approved organization sender IDs and the approved platform default can be used to send messages. Super administrators review requests.

### 6. Sending SMS

Single and bulk sending support raw phone numbers and saved contact groups.

For an immediate send, the backend:

1. Resolves and de-duplicates recipients.
2. Normalizes Kenyan phone numbers to `254XXXXXXXXX`.
3. Validates the sender ID.
4. Checks the organization's access and wallet balance.
5. Debits the wallet.
6. Stores the SMS record.
7. Sends through Africa's Talking.
8. Records provider requests, responses, retries, and final status.
9. Applies delivery reports and refunds according to the failure path.

Typical message states are:

```text
QUEUED → SENT → DELIVERED
              → FAILED
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

Africa's Talking posts delivery updates to:

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

### API key

An organization API key is returned during registration. Programmatic clients can send:

```http
X-API-Key: nsk_...
```

API keys are organization-scoped and must be stored securely.

## Main API areas

- `POST /api/v1/organizations/register` — create an organization, administrator, wallet, and account references
- `POST /api/v1/auth/login` — authenticate a dashboard user
- `GET /api/v1/auth/me` — get the authenticated user's profile
- `POST /api/v1/auth/change-password` — change the authenticated user's password
- `POST /api/v1/auth/forgot-password` — request a one-time password-reset link
- `POST /api/v1/auth/reset-password` — reset a password with the one-time token
- `GET /api/v1/wallet/balance` — get wallet balance and SMS cost
- `POST /api/v1/wallet/topup` — initiate M-Pesa STK Push
- `GET /api/v1/wallet/topup/{transactionId}` — read top-up status
- `POST /api/v1/wallet/topup/{transactionId}/check` — query and reconcile pending top-up status
- `GET /api/v1/wallet/transactions` — list wallet activity
- `POST /api/v1/sms/send` — send one SMS
- `POST /api/v1/sms/bulk` — send to numbers and/or a contact group
- `POST /api/v1/sms/schedule` — schedule messages for later
- `GET /api/v1/sms/history` — list sent and scheduled messages
- `/api/v1/contacts/**` — contacts, groups, imports, and template download
- `/api/v1/sender-ids/**` — sender-ID requests and reviews
- `GET /api/v1/reports/dashboard` — organization dashboard metrics
- `/api/v1/admin/**` — super-administrator operations

Full request and response schemas are available in Swagger UI.

## Database and startup behavior

Flyway migrations currently cover:

- Initial organizations, users, wallets, SMS, contacts, sender IDs, and provider logs
- M-Pesa Daraja transaction and organization account-reference fields
- Business and event organization account types

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

## Production checklist

- Replace all example credentials and secrets.
- Move datasource and JWT configuration to environment-specific secrets.
- Use production Africa's Talking and Daraja endpoints.
- Serve the API through HTTPS.
- Set the public M-Pesa and DLR callback URLs.
- Restrict CORS to the deployed frontend origin.
- Back up MySQL and monitor Flyway migrations.
- Monitor `/actuator/health`, provider errors, pending top-ups, and scheduled-message dispatch.
- Build the frontend with `VITE_USE_MOCK=false`.
- Keep API keys, JWTs, M-Pesa credentials, and provider credentials out of source control.

## Additional documentation

- Frontend notes: `Nova sms front/README.md`
- Backend and API examples: `Novastack Sms back/Novastack-Sms/README.md`
