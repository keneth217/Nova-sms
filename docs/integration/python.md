# Python integration

```python
import os
import requests

response = requests.post(
    f"{os.environ['NOVA_SMS_API_URL']}/api/v1/sms/send",
    headers={
        "X-API-Key": os.environ["NOVA_SMS_API_KEY"],
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Idempotency-Key": "payment-123456",
    },
    json={
        "recipient": "254712345678",
        "message": "Payment received.",
    },
    timeout=30,
)

data = response.json()
print(data)

if not response.ok or not data.get("success"):
    raise RuntimeError(data.get("message") or f"Nova SMS HTTP {response.status_code}")

message_id = data["data"]["id"]
status = data["data"]["status"]
```

Keep `NOVA_SMS_API_KEY` in the environment. Do not put it in a Django/Flask template or a browser bundle.
