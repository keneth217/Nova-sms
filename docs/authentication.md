# Authentication

Nova SMS has two authentication methods. **API integrations use API keys.** Dashboard users use email/password and a JWT.

## API keys

Send the plaintext key on every request:

```http
X-API-Key: nova_live_xxxxxxxxxxxxxxxxx
```

Example:

```bash
export NOVA_SMS_API_KEY="nova_live_xxxxxxxxx"
```

### How keys are issued (Super Admin)

1. Open Super Admin.
2. Go to **Developer**.
3. Open **API Clients**.
4. Click **New API client** (Create API Client).
5. Select the organization (for example Mwalimu or Chamaplus).
6. Enter the application name.
7. Select permissions (`SMS_SEND`, `SMS_BULK`, `SMS_STATUS`, `SMS_HISTORY`).
8. Configure the per-minute rate limit (default 100).
9. Create the API client.
10. Copy the API key immediately. The full key is shown **only once**.
11. Store it in a secrets manager or environment variable. Nova stores only a SHA-256 hash.

Organization administrators can create clients for **their** organization from Dashboard → API clients.

Revoked keys are rotated to a random hash and stop working immediately. Rotate to issue a new key; the previous plaintext stops working at once.

### Ownership

- API keys belong to **API clients**.
- API clients belong to **organizations**.
- API keys are **not** user passwords and are not used to log into the dashboard.

### Scoped permissions

Hashed live keys are scoped. They may only call `/api/v1/sms/**`, and only with granted permissions:

| Permission     | Endpoints |
| -------------- | --------- |
| `SMS_SEND`     | `POST /api/v1/sms/send` |
| `SMS_BULK`     | `POST /api/v1/sms/bulk`, `POST /api/v1/sms/schedule` |
| `SMS_STATUS`   | `GET /api/v1/sms/{id}`, `GET /api/v1/sms/{id}/status` |
| `SMS_HISTORY`  | `GET /api/v1/sms/history` |

Default permissions when none are specified: `SMS_SEND`, `SMS_BULK`, `SMS_STATUS`.

Missing permission returns HTTP **403**:

```json
{ "success": false, "message": "API key is missing permission SMS_HISTORY" }
```

A scoped key used outside `/api/v1/sms` returns HTTP **403**:

```json
{ "success": false, "message": "This API key cannot access that resource" }
```

### Invalid or revoked keys

If `X-API-Key` is present but the key is unknown, disabled, revoked, expired, or the organization is inactive, Nova returns HTTP **401**:

```json
{ "success": false, "message": "Invalid API key." }
```

### Legacy organization keys

Older `nsk_…` organization keys still authenticate as the full organization (not scoped). New integrations should use `nova_live_…` API clients.

## JWT (dashboard only)

```http
Authorization: Bearer <accessToken>
```

Issued by `POST /api/v1/auth/login`. Do not use dashboard JWTs in server-to-server integrations.

## Storing keys

- Store keys in environment variables or a secrets manager.
- Never commit keys to Git.
- Never put keys in frontend/browser code.
- Frontend apps must call **your** backend; your backend calls Nova SMS.
