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

## Wallet balance and top-up

Grant `WALLET_READ` and `WALLET_TOPUP` on the API client, then expose these from **your** backend so users can fund SMS on your site.

```javascript
export async function getSmsBalance() {
  const response = await fetch(
    `${process.env.NOVA_SMS_API_URL}/api/v1/wallet/balance`,
    {
      headers: {
        "X-API-Key": process.env.NOVA_SMS_API_KEY,
        "Accept": "application/json"
      }
    }
  );
  const body = await response.json();
  if (!response.ok || !body.success) {
    throw new Error(body.message || `Nova SMS HTTP ${response.status}`);
  }
  return body.data;
}

export async function startSmsTopup(amount, phoneNumber) {
  const response = await fetch(
    `${process.env.NOVA_SMS_API_URL}/api/v1/wallet/topup`,
    {
      method: "POST",
      headers: {
        "X-API-Key": process.env.NOVA_SMS_API_KEY,
        "Content-Type": "application/json",
        "Accept": "application/json"
      },
      body: JSON.stringify({ amount, phoneNumber })
    }
  );
  const body = await response.json();
  if (!response.ok || !body.success) {
    throw new Error(body.message || `Nova SMS HTTP ${response.status}`);
  }
  return body.data;
}
```

```javascript
export async function waitForSmsTopup(transactionId) {
  await new Promise((resolve) => setTimeout(resolve, 5000));
  while (true) {
    const response = await fetch(
      `${process.env.NOVA_SMS_API_URL}/api/v1/wallet/topup/${transactionId}/check`,
      {
        method: "POST",
        headers: {
          "X-API-Key": process.env.NOVA_SMS_API_KEY,
          "Accept": "application/json"
        }
      }
    );
    const body = await response.json();
    if (!response.ok || !body.success) {
      throw new Error(body.message || `Nova SMS HTTP ${response.status}`);
    }
    const data = body.data;
    if (data.status === "COMPLETED" && data.walletCredited) {
      return getSmsBalance();
    }
    if (data.status === "FAILED") {
      throw new Error(data.resultDesc || "Top-up failed");
    }
    await new Promise((resolve) => setTimeout(resolve, 4000));
  }
}
```

Polling rules: wait ~5 seconds, then `POST …/check` every 3–5 seconds while `PENDING`. `"The transaction is still under processing"` is `PENDING`, not failure. Stop when `COMPLETED` and `walletCredited=true`, or on definitive `FAILED`. Do not start another STK Push while this transaction is `PENDING`. `GET /api/v1/wallet/topup/{id}` recovers the stored row only — it does not query Safaricom. See [Wallet](../api/wallet.md).
