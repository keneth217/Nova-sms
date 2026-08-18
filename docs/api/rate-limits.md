# Rate limits

Rate limits belong to **API clients**, not to the whole platform.

- Super Admin (or an organization admin) sets `rateLimitPerMinute` when creating or updating a client.
- Allowed range: **1–10,000** requests per minute.
- Default when omitted: **100** requests per minute.
- Limits are enforced in memory on the API process (sliding 60-second window).
- JWT dashboard traffic is not counted against an API client bucket.

When exceeded, Nova returns HTTP **429**:

```json
{
  "success": false,
  "message": "Too many API requests. Please wait a minute and try again."
}
```

Retry after waiting. Do not tight-loop.

There is no universal platform quota of “100 requests/minute for every app”. That number is only the **default** for a new API client. Example: an Mwalimu client might be configured at 100/minute; another client can be set higher or lower by Super Admin.
