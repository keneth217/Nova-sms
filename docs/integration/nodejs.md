# Node.js integration

Call Nova SMS from a **Node.js server** (Express, Nest, Next.js Route Handler, etc.). Never ship the API key to the browser.

```javascript
const response = await fetch(
  `${process.env.NOVA_SMS_API_URL}/api/v1/sms/send`,
  {
    method: "POST",
    headers: {
      "X-API-Key": process.env.NOVA_SMS_API_KEY,
      "Content-Type": "application/json",
      "Accept": "application/json",
      "Idempotency-Key": "payment-123456"
    },
    body: JSON.stringify({
      recipient: "254712345678",
      message: "Payment received."
    })
  }
);

const data = await response.json();

if (!response.ok || !data.success) {
  throw new Error(data.message || `Nova SMS HTTP ${response.status}`);
}

const messageId = data.data.id;
const status = data.data.status;
```

`NOVA_SMS_API_URL` is the origin (`https://smsapi.novastack.co.ke` or `http://localhost:8092`).
