# Errors

Every error uses the same envelope. There is **no** `code` field.

```json
{
  "success": false,
  "message": "Human-readable explanation."
}
```

## HTTP status codes actually returned

### Missing API key (or unauthenticated request)

HTTP **401**

When `X-API-Key` is absent and there is no valid JWT:

```json
{
  "success": false,
  "message": "Session expired. Please sign in again."
}
```

### Invalid, revoked, disabled, or expired API key

HTTP **401**

When `X-API-Key` is present but not accepted:

```json
{
  "success": false,
  "message": "Invalid API key."
}
```

### Insufficient wallet balance

HTTP **402**

```json
{
  "success": false,
  "message": "Insufficient wallet balance"
}
```

A more detailed variant may include the required amount:

```json
{
  "success": false,
  "message": "Insufficient wallet balance. Required: 3.00 KES"
}
```

### Forbidden (scoped key)

HTTP **403**

Wrong resource (not `/api/v1/sms/**`):

```json
{
  "success": false,
  "message": "This API key cannot access that resource"
}
```

Missing permission:

```json
{
  "success": false,
  "message": "API key is missing permission SMS_HISTORY"
}
```

Other authorization failures:

```json
{
  "success": false,
  "message": "Access denied"
}
```

### Validation and invalid phone

HTTP **400** (not 422)

Invalid Kenyan mobile:

```json
{
  "success": false,
  "message": "Invalid phone number 'abc'. Use 07…, 01…, 254…, or +254…"
}
```

Blank required fields use Jakarta validation text (for example `"must not be blank"`). Unapproved sender ID: `"Sender ID is not approved for this organization"`. Empty bulk: `"No recipients provided"`.

### Idempotency conflict

HTTP **409**

```json
{
  "success": false,
  "message": "Idempotency-Key was reused with a different request body"
}
```

### Rate limit

HTTP **429**

```json
{
  "success": false,
  "message": "Too many API requests. Please wait a minute and try again."
}
```

### Message not found

HTTP **404**

```json
{
  "success": false,
  "message": "SMS message not found"
}
```

### Provider unavailable

HTTP **502** (uncaught provider HTTP failures)

```json
{
  "success": false,
  "message": "SMS provider is temporarily unavailable. Please try again."
}
```

Send-time provider failures more often appear as HTTP **200** with `data.status` of `FAILED` and `data.failureReason` set to a customer-facing provider message (timeout, validation, unavailable, and similar). The wallet is refunded for those failures.

### Unexpected error

HTTP **500**

```json
{
  "success": false,
  "message": "Unexpected error. Please try again."
}
```
