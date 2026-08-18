# TalkSasa provider

**INTERNAL ONLY.** Do not give this document to integrating developers. Public Nova SMS documentation must not require a TalkSasa account.

```text
Nova SMS
   ↓
TalkSasaSmsProvider
   ↓
TalkSasa Bulk SMS API v3
```

Integrating apps call Nova SMS only. TalkSasa credentials stay on the Nova SMS server (`TALKSASA_API_TOKEN`). Never put that token in Git, logs, the frontend, or public docs.

## Provider

| Item | Value |
| ---- | ----- |
| Adapter | `TalkSasaSmsProvider` |
| Config key | `novastack.sms.talksasa` / `SMS_PROVIDER=talksasa` |
| Base URL | `TALKSASA_BASE_URL` default `https://bulksms.talksasa.com/api/v3` |
| Authentication | `Authorization: Bearer {TALKSASA_API_TOKEN}` |
| Send | `POST /sms/send` |
| Status | `GET /sms/queue/{queue_uid}` (fallback `GET /sms/{uid}`) |
| Provider message id | TalkSasa `queue_uid` or `uid`, stored on `sms_messages.provider_message_id` |
| Default sender | `TALKSASA_SENDER_ID` (default `TALK-SASA`). Configurable; do not hard-code in application logic. |

When Nova SMS receives a send with no `senderId`, `TalkSasaSmsProvider` is given this configured default and TalkSasa receives `"sender_id": "TALK-SASA"` (unless the env override is set).

## Error handling

Vendor HTTP errors are mapped to customer-facing strings in `ProviderErrorMessages` (unavailable, timeout, authentication, validation, rate limit). Raw TalkSasa bodies are redacted in logs and are **not** returned as the public Nova SMS response.

## Retry behavior

`SmsDeliveryService` retries retryable failures up to `novastack.sms.max-retries` (default 3). Retryable HTTP statuses: 408, 502, 503, 504, plus network/timeouts. Non-retryable failures mark the Nova message `FAILED`/`REJECTED` and refund the wallet.

Bulk sends are chunked with `SMS_BATCH_SIZE` (default 100 recipients per TalkSasa request).

## Status mapping

TalkSasa vendor statuses are mapped by `TalkSasaStatusMapper` onto Nova `MessageStatus` (`ACCEPTED`, `SENT`, `DELIVERED`, `FAILED`, `REJECTED`, `CANCELLED`, `PENDING`). A scheduled job (`SMS_STATUS_SYNC_CRON`, default every 5 minutes) refreshes in-flight messages.

## Contact groups (internal)

When `TALKSASA_SYNC_CONTACT_GROUPS=true`, Nova mirrors groups and grouped contacts to TalkSasa. Nova remains the source of truth. SMS is still sent from Nova recipient lists, not TalkSasa campaigns.

## Super Admin

`GET /api/v1/admin/talksasa` shows the platform TalkSasa profile and unit balance. That endpoint is Super Admin only.
