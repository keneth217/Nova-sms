# Nova SMS documentation

Nova SMS is a multi-tenant SMS SaaS product and an SMS gateway for other applications.

Integrating applications (Mwalimu Scheme, Chamaplus, Nova POS, SACCO and school systems, other NovaStack apps, and third-party backends) talk **only to Nova SMS**. They never call TalkSasa or any other upstream provider.

## Start here

1. [Architecture](architecture.md) — how SaaS users and API clients share one SMS engine
2. [Authentication](authentication.md) — `X-API-Key` live keys
3. [API reference](api/README.md) — send, bulk, status, history, errors
4. [Integration guides](integration/README.md) — Spring Boot, Node.js, PHP, Python, generic HTTP

Internal operator notes (not for integrating developers):

- [TalkSasa provider](providers/talksasa.md) — **INTERNAL ONLY**

## Public URLs

| Resource | Location |
| -------- | -------- |
| Interactive docs in the app | Super Admin → Developer |
| Public developer page | `/developers` |
| OpenAPI JSON | `{API origin}/v3/api-docs` |
| Swagger UI | `{API origin}/swagger-ui.html` |

The API origin is configured with `NOVA_SMS_API_BASE_URL` (default `https://smsapi.novastack.co.ke`). Paths are under `/api/v1`.

Default provider: **TalkSasa**. Default sender ID when `senderId` is omitted: **TALK-SASA** (`TALKSASA_SENDER_ID`, configurable).

## Security

Never put a Nova SMS API key in frontend JavaScript, never commit it to Git, and never expose it to browser users. Call Nova SMS from **your backend**.
