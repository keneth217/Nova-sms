import type { DocBlock, DocPage } from '@/data/developer-docs'

export function buildMpesaDocPages(origin: string): Record<string, DocPage> {
  const o = origin.replace(/\/$/, '')

  return {
    mpesa: {
      id: 'mpesa',
      title: 'M-Pesa API',
      description:
        'Fund the organization SMS wallet with STK Push, checkout, or Paybill C2B. Clients do not configure or implement Safaricom callbacks.',
      blocks: [
        {
          type: 'p',
          text: 'Clients do not configure or implement Safaricom callbacks. Nova SMS handles Safaricom STK and C2B callbacks internally. Client applications initiate payments through the Nova SMS API and retrieve transaction status through the authenticated API.',
        },
        {
          type: 'p',
          text: 'Each application (Mwalimu, Chamaplus, POS, and others) uses its own X-API-Key. You do not need Daraja callback structure, Safaricom credentials, STK parsing, C2B validation, receipt extraction, callback security, or reconciliation. C2B URL registration is Super Admin only.',
        },
        { type: 'h2', text: 'Base URL' },
        { type: 'code', language: 'text', code: o },
        {
          type: 'note',
          text: 'This is smsapi.novastack.co.ke, not a NovaPay host. Use the SMS and M-Pesa tabs in Super Admin → Developer for the same docs that partners follow.',
        },
        { type: 'h2', text: 'Client-facing endpoints' },
        {
          type: 'table',
          headers: ['Method', 'Path', 'Permission', 'Purpose'],
          rows: [
            ['POST', '/api/v1/mpesa/stkpush', 'MPESA_STK_PUSH', 'Initiate STK Push'],
            ['POST', '/api/v1/mpesa/checkout', 'MPESA_STK_PUSH', 'Lipa Na M-Pesa Online Checkout (same as STK)'],
            ['GET', '/api/v1/mpesa/transactions/{id}', 'MPESA_STATUS', 'Stored STK transaction'],
            ['GET', '/api/v1/mpesa/transactions/{id}/status', 'MPESA_STATUS', 'Poll STK until credited or failed'],
            ['GET', '/api/v1/mpesa/c2b/transactions', 'MPESA_C2B', 'List Paybill C2B credits'],
            ['GET', '/api/v1/mpesa/c2b/transactions/{id}', 'MPESA_C2B', 'Get one C2B credit'],
          ],
        },
        { type: 'h2', text: 'Authentication' },
        {
          type: 'p',
          text: 'Like SMS, these routes are not public. Portal users send Authorization: Bearer. External apps send X-API-Key. The key identifies the organization — do not send organizationId.',
        },
        {
          type: 'code',
          language: 'http',
          code: `X-API-Key: nova_live_xxxxxxxxx
Content-Type: application/json
Accept: application/json`,
        },
        {
          type: 'ul',
          items: [
            'Missing permission → HTTP 403.',
            'Rate limit exceeded → HTTP 429.',
            'Invalid or missing API key → HTTP 401.',
            'MPESA_STK_PUSH initiates STK and checkout. MPESA_STATUS reads STK status. MPESA_C2B lists C2B transactions. WALLET_TOPUP still implies STK, status, and C2B. WALLET_READ implies C2B.',
          ],
        },
        { type: 'h2', text: 'Architecture' },
        {
          type: 'pre',
          text: `                         ┌──────────────────┐
                         │   Mwalimu Scheme │
                         └────────┬─────────┘
                                  │ X-API-Key
                                  ▼
┌──────────────┐        ┌────────────────────┐
│   Chamaplus  │───────▶│      NOVA SMS      │
└──────────────┘        │  SMS / M-Pesa /    │
┌──────────────┐        │  Wallet APIs       │
│     POS      │───────▶│                    │
└──────────────┘        └─────────┬──────────┘
                                  │ Nova-managed credentials
                                  ▼
                         ┌────────────────┐
                         │   Safaricom    │
                         │    Daraja      │
                         └───────┬────────┘
                    STK Push     │     C2B
                    ▼            │      ▼
                Customer         │   Paybill
                    └────────────┴────────────┘
                                 │ Callback
                                 ▼
                         ┌────────────────┐
                         │ NOVA SMS       │
                         │ callback layer │
                         └────────────────┘`,
        },
        {
          type: 'p',
          text: 'Dashboard JWT users and API-key clients call the same WalletService. Issue one key per integrating application. Language examples are under Call from your app on this M-Pesa tab.',
        },
      ],
    },
    'mpesa-stk': {
      id: 'mpesa-stk',
      title: 'STK Push',
      description:
        'POST /api/v1/mpesa/stkpush sends a Lipa Na M-Pesa prompt. Permission MPESA_STK_PUSH (or WALLET_TOPUP).',
      blocks: stkBlocks(o),
    },
    'mpesa-checkout': {
      id: 'mpesa-checkout',
      title: 'Checkout',
      description:
        'POST /api/v1/mpesa/checkout is Lipa Na M-Pesa Online — the same request as STK Push.',
      blocks: checkoutBlocks(),
    },
    'mpesa-status': {
      id: 'mpesa-status',
      title: 'Payment status',
      description:
        'Poll Nova SMS, not Safaricom. GET /api/v1/mpesa/transactions/{id}/status until COMPLETED and walletCredited is true.',
      blocks: statusBlocks(o),
    },
    'mpesa-c2b': {
      id: 'mpesa-c2b',
      title: 'Paybill C2B',
      description:
        'GET /api/v1/mpesa/c2b returns Paybill and account. After the customer pays, POST /api/v1/mpesa/c2b/verify with the receipt.',
      blocks: c2bBlocks(o),
    },
    'mpesa-callbacks': {
      id: 'mpesa-callbacks',
      title: 'Callbacks',
      description:
        'Safaricom posts STK and C2B callbacks to Nova SMS. Integrating apps do not receive those posts.',
      blocks: callbackBlocks(),
    },
  }
}

function stkBlocks(o: string): DocBlock[] {
  return [
    { type: 'http', method: 'POST', path: '/api/v1/mpesa/stkpush' },
    {
      type: 'table',
      headers: ['Field', 'Required', 'Type', 'Description'],
      rows: [
        ['amount', 'Yes', 'number', 'KES. Minimum 1.00'],
        ['phoneNumber', 'Yes', 'string', 'M-Pesa phone that receives the STK prompt. 07…, 254…, or +254…'],
      ],
    },
    {
      type: 'p',
      text: 'Clients do not send accountReference or transactionDesc. Nova uses the organization Paybill account (for example NOVAC727) and the platform shortcode.',
    },
    {
      type: 'code',
      language: 'bash',
      code: `curl -X POST "${o}/api/v1/mpesa/stkpush" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Idempotency-Key: learner-839-payment-2026-08-24" \\
  -H "Content-Type: application/json" \\
  -H "Accept: application/json" \\
  -d '{
    "amount": 500,
    "phoneNumber": "254712345678"
  }'`,
    },
    {
      type: 'p',
      text: 'Nova authenticates the key, creates a PENDING wallet_transactions row, sends Lipa Na M-Pesa STK through Daraja, stores checkoutRequestId, and returns transactionId. The wallet is not credited yet. Do not treat this HTTP response as payment success.',
    },
    {
      type: 'code',
      language: 'json',
      code: `{
  "success": true,
  "message": "STK Push sent. Enter M-Pesa PIN on your phone.",
  "data": {
    "transactionId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "checkoutRequestId": "ws_CO_19082026104512345",
    "status": "PENDING",
    "amount": 500.00,
    "phoneNumber": "254712345678",
    "callbackReceived": false,
    "walletCredited": false
  }
}`,
    },
    {
      type: 'warn',
      text: 'Idempotency-Key on STK or checkout replays the original transaction. Same key + different body returns HTTP 409. stkpush and checkout share the same idempotency hash.',
    },
  ]
}

function checkoutBlocks(): DocBlock[] {
  return [
    { type: 'http', method: 'POST', path: '/api/v1/mpesa/checkout' },
    {
      type: 'p',
      text: 'Same body as STK Push: amount and phoneNumber. Same WalletService and the same Idempotency-Key hash, so a key used on /stkpush replays on /checkout.',
    },
    {
      type: 'table',
      headers: ['Method', 'Path', 'Purpose'],
      rows: [
        ['POST', '/api/v1/mpesa/checkout', 'Start Lipa Na M-Pesa Online (same as STK)'],
        ['GET', '/api/v1/mpesa/checkout/{id}', 'Stored row only'],
        ['GET', '/api/v1/mpesa/checkout/{id}/status', 'Refresh; query Safaricom if PENDING'],
      ],
    },
    {
      type: 'p',
      text: 'Poll GET /api/v1/mpesa/checkout/{id}/status or GET /api/v1/mpesa/transactions/{id}/status until COMPLETED and walletCredited is true, or FAILED.',
    },
  ]
}

function statusBlocks(o: string): DocBlock[] {
  return [
    { type: 'http', method: 'GET', path: '/api/v1/mpesa/transactions/{id}/status' },
    { type: 'http', method: 'GET', path: '/api/v1/mpesa/checkout/{id}/status' },
    { type: 'http', method: 'GET', path: '/api/v1/mpesa/transactions/{id}' },
    {
      type: 'p',
      text: 'GET …/status reads MySQL and, if still PENDING, queries Daraja STK query and updates the row. GET …/{id} only reads the stored row. Poll Nova SMS, not Safaricom. Do not wait for a callback on your server — Nova owns those URLs.',
    },
    {
      type: 'code',
      language: 'bash',
      code: `curl -X GET "${o}/api/v1/mpesa/transactions/\${TRANSACTION_ID}/status" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Accept: application/json"`,
    },
    {
      type: 'table',
      headers: ['status', 'Meaning', 'Your app'],
      rows: [
        ['PENDING', 'STK sent, or Safaricom still processing. Not a failure.', 'Keep polling.'],
        ['COMPLETED', 'Payment succeeded. Wallet credited once.', 'Stop only when walletCredited is also true.'],
        ['FAILED', 'Cancelled, insufficient funds, wrong PIN, or similar. There is no CANCELLED status.', 'Stop. Show failure.'],
      ],
    },
    {
      type: 'p',
      text: 'Treat the top-up as paid only when status is COMPLETED and walletCredited is true.',
    },
    {
      type: 'pre',
      text: `Client
   │
   │ POST /api/v1/mpesa/stkpush
   ▼
Nova SMS API
   │
   │ Daraja STK Push
   ▼
Safaricom
   │
   │ STK prompt
   ▼
Customer
   │
   │ M-Pesa PIN
   ▼
Safaricom
   │
   │ POST /api/v1/mpesa/stk/callback
   ▼
Nova SMS API
   │
   ▼
wallet_transactions = COMPLETED / FAILED`,
    },
  ]
}

function c2bBlocks(o: string): DocBlock[] {
  return [
    { type: 'http', method: 'GET', path: '/api/v1/mpesa/c2b/transactions' },
    { type: 'http', method: 'GET', path: '/api/v1/mpesa/c2b/transactions/{id}' },
    {
      type: 'p',
      text: 'There is no client POST to start a C2B payment and no client C2B registration. Nova handles the Safaricom confirmation. List or get credits with the authenticated API. Optional GET /api/v1/mpesa/c2b shows paybill and account to the customer. Optional POST /c2b/verify looks up a receipt.',
    },
    {
      type: 'code',
      language: 'bash',
      code: `curl -X GET "${o}/api/v1/mpesa/c2b/transactions" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Accept: application/json"`,
    },
    {
      type: 'note',
      text: 'Registering C2B URLs with Daraja is Super Admin only: POST /api/v1/admin/mpesa/c2b/register. Ordinary API clients never get that endpoint.',
    },
  ]
}

function callbackBlocks(): DocBlock[] {
  return [
    {
      type: 'p',
      text: 'Clients do not configure or implement Safaricom callbacks. Nova SMS handles Safaricom STK and C2B callbacks internally. Client applications initiate payments through the Nova SMS API and retrieve transaction status through the authenticated API.',
    },
    {
      type: 'p',
      text: 'You do not implement Daraja callback structure, Safaricom credentials, STK callback parsing, C2B validation, M-Pesa receipt extraction, callback security, or transaction reconciliation. You only integrate with Nova SMS.',
    },
    {
      type: 'table',
      headers: ['Flow', 'What you call', 'What you never call'],
      rows: [
        ['STK / checkout', 'GET /api/v1/mpesa/transactions/{id}/status', 'POST /api/v1/mpesa/stk/callback'],
        ['Paybill C2B', 'GET /api/v1/mpesa/c2b/transactions', 'POST /api/v1/payments/c2b/confirmation'],
      ],
    },
    {
      type: 'p',
      text: 'If a Paybill callback is delayed, POST /api/v1/mpesa/c2b/verify may return source SAFARICOM_QUERY. Nova is asking Safaricom Transaction Status internally. Wait a few seconds and verify or list C2B transactions again. You never call Daraja Transaction Status yourself.',
    },
    {
      type: 'note',
      text: 'A later Nova webhook may send payment.success, payment.failed, and c2b.received. Polling remains the supported fallback.',
    },
  ]
}
