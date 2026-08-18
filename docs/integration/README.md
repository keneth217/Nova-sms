# Integration guides

Call Nova SMS from **your backend**. Do not put the API key in a browser.

```text
Browser
   ↓
Your Backend
   ↓
Nova SMS API
   ↓
(TalkSasa — internal to Nova)
```

Environment variables used in these examples:

```bash
export NOVA_SMS_API_URL="https://smsapi.novastack.co.ke"
export NOVA_SMS_API_KEY="nova_live_xxxxxxxxx"
```

Local development uses `http://localhost:8092` as `NOVA_SMS_API_URL`.

- [Spring Boot](spring-boot.md)
- [Node.js](nodejs.md)
- [PHP](php.md)
- [Python](python.md)
- [Generic HTTP](generic-http.md)

Integrating applications do **not** create a TalkSasa account. Nova SMS is the only SMS API they need.
