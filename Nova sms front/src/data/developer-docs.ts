import { buildMpesaDocPages } from '@/data/developer-mpesa-pages'

export const FALLBACK_API_ORIGIN = 'https://smsapi.novastack.co.ke'

export function originFromVite(): string {
  const base = import.meta.env.VITE_API_BASE_URL || `${FALLBACK_API_ORIGIN}/api/v1`
  return base.replace(/\/api\/v1\/?$/, '').replace(/\/$/, '') || FALLBACK_API_ORIGIN
}

export type DocBlock =
  | { type: 'p'; text: string }
  | { type: 'h2'; text: string }
  | { type: 'h3'; text: string }
  | { type: 'ul'; items: string[] }
  | { type: 'ol'; items: string[] }
  | { type: 'warn'; text: string }
  | { type: 'note'; text: string }
  | { type: 'code'; language?: string; code: string }
  | { type: 'http'; method: string; path: string }
  | { type: 'table'; headers: string[]; rows: string[][] }
  | { type: 'pre'; text: string }

export interface DocPage {
  id: string
  title: string
  description: string
  blocks: DocBlock[]
}

export interface DocNavItem {
  id: string
  label: string
  to: string
}

export interface DocNavGroup {
  label: string
  items: DocNavItem[]
}

export type DeveloperDocTab = 'sms' | 'mpesa'

export const developerTabs: { id: DeveloperDocTab; label: string; to: string }[] = [
  { id: 'sms', label: 'SMS', to: '/admin/developer' },
  { id: 'mpesa', label: 'M-Pesa', to: '/admin/developer/mpesa' },
]

export const smsDocNav: DocNavGroup[] = [
  {
    label: 'Getting started',
    items: [
      { id: 'overview', label: 'Overview', to: '/admin/developer' },
      { id: 'quick-start', label: 'Quick Start', to: '/admin/developer/quick-start' },
      { id: 'authentication', label: 'Authentication', to: '/admin/developer/authentication' },
    ],
  },
  {
    label: 'SMS API',
    items: [
      { id: 'send-sms', label: 'Send SMS', to: '/admin/developer/send-sms' },
      { id: 'bulk-sms', label: 'Bulk SMS', to: '/admin/developer/bulk-sms' },
      { id: 'retry-failed', label: 'Retry failed SMS', to: '/admin/developer/retry-failed' },
      { id: 'status', label: 'SMS Status', to: '/admin/developer/status' },
      { id: 'history', label: 'SMS History', to: '/admin/developer/history' },
    ],
  },
  {
    label: 'Platform',
    items: [
      { id: 'errors', label: 'Errors', to: '/admin/developer/errors' },
      { id: 'idempotency', label: 'Idempotency', to: '/admin/developer/idempotency' },
      { id: 'rate-limits', label: 'Rate Limits', to: '/admin/developer/rate-limits' },
    ],
  },
  {
    label: 'Integration',
    items: [
      { id: 'spring-boot', label: 'Spring Boot', to: '/admin/developer/integration/spring-boot' },
      { id: 'nodejs', label: 'Node.js', to: '/admin/developer/integration/nodejs' },
      { id: 'php', label: 'PHP', to: '/admin/developer/integration/php' },
      { id: 'python', label: 'Python', to: '/admin/developer/integration/python' },
      { id: 'generic-http', label: 'Generic HTTP', to: '/admin/developer/integration/generic-http' },
    ],
  },
  {
    label: 'Internal',
    items: [{ id: 'provider', label: 'TalkSasa (internal)', to: '/admin/developer/provider' }],
  },
]

export const mpesaDocNav: DocNavGroup[] = [
  {
    label: 'M-Pesa API',
    items: [
      { id: 'mpesa', label: 'Overview', to: '/admin/developer/mpesa' },
      { id: 'authentication', label: 'Authentication', to: '/admin/developer/mpesa/authentication' },
      { id: 'mpesa-stk', label: 'STK Push', to: '/admin/developer/mpesa/stk' },
      { id: 'mpesa-checkout', label: 'Checkout', to: '/admin/developer/mpesa/checkout' },
      { id: 'mpesa-status', label: 'Payment status', to: '/admin/developer/mpesa/status' },
      { id: 'mpesa-c2b', label: 'Paybill C2B', to: '/admin/developer/mpesa/c2b' },
      { id: 'mpesa-callbacks', label: 'Callbacks', to: '/admin/developer/mpesa/callbacks' },
      { id: 'wallet', label: 'Wallet', to: '/admin/developer/wallet' },
    ],
  },
  {
    label: 'Call from your app',
    items: [
      { id: 'spring-boot', label: 'Spring Boot', to: '/admin/developer/mpesa/integration/spring-boot' },
      { id: 'nodejs', label: 'Node.js', to: '/admin/developer/mpesa/integration/nodejs' },
      { id: 'php', label: 'PHP', to: '/admin/developer/mpesa/integration/php' },
      { id: 'python', label: 'Python', to: '/admin/developer/mpesa/integration/python' },
      { id: 'generic-http', label: 'Generic HTTP', to: '/admin/developer/mpesa/integration/generic-http' },
    ],
  },
]

export const developerManageNav: DocNavGroup = {
  label: 'Manage',
  items: [
    { id: 'clients', label: 'API Clients', to: '/admin/developer/clients' },
    { id: 'usage', label: 'API Usage', to: '/admin/developer/usage' },
    { id: 'console', label: 'Test Console', to: '/admin/developer/console' },
  ],
}

/** Combined admin nav (SMS + M-Pesa + Manage). Prefer navForDeveloperTab in the portal. */
export const developerNav: DocNavGroup[] = [...smsDocNav, ...mpesaDocNav, developerManageNav]

export function navForDeveloperTab(tab: DeveloperDocTab): DocNavGroup[] {
  const docs = tab === 'mpesa' ? mpesaDocNav : smsDocNav
  return [...docs, developerManageNav]
}

export function developerTabForPath(path: string): DeveloperDocTab {
  if (path === '/admin/developer/wallet' || path.startsWith('/admin/developer/wallet/')) {
    return 'mpesa'
  }
  if (path === '/admin/developer/mpesa' || path.startsWith('/admin/developer/mpesa/')) {
    return 'mpesa'
  }
  return 'sms'
}

export function publicDocPath(id: string): string {
  return id === 'overview' ? '/developers' : `/developers/${id}`
}

function toPublicGroup(group: DocNavGroup): DocNavGroup {
  return {
    label: group.label,
    items: group.items.map((item) => ({
      ...item,
      to: publicDocPath(item.id),
    })),
  }
}

export const publicDeveloperNav: DocNavGroup[] = [
  ...smsDocNav.filter((group) => group.label !== 'Internal').map(toPublicGroup),
  ...mpesaDocNav.map(toPublicGroup),
]

export function buildDeveloperPages(origin: string): Record<string, DocPage> {
  const o = origin.replace(/\/$/, '')
  const sendCurl = `curl -X POST "${o}/api/v1/sms/send" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Content-Type: application/json" \\
  -H "Accept: application/json" \\
  -d '{
    "recipient": "254712345678",
    "message": "Your payment has been received."
  }'`
  const sendSenderCurl = `curl -X POST "${o}/api/v1/sms/send" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Content-Type: application/json" \\
  -H "Accept: application/json" \\
  -d '{
    "recipient": "254712345678",
    "senderId": "CHAMAPLUS",
    "message": "Your payment has been received."
  }'`
  const helloCurl = `curl -X POST "${o}/api/v1/sms/send" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "recipient": "254712345678",
    "message": "Hello from Nova SMS."
  }'`

  return {
    ...buildMpesaDocPages(o),
    overview: {
      id: 'overview',
      title: 'API Overview',
      description: 'Nova SMS REST API for integrating applications.',
      blocks: [
        {
          type: 'p',
          text: 'Nova SMS provides a REST API for other applications — Mwalimu, Chamaplus, Nova POS, SACCOs, school systems, and any third-party backend. Those apps never talk to TalkSasa or Safaricom Daraja. Each application gets its own API client and nova_live_ key.',
        },
        {
          type: 'p',
          text: 'Dashboard users keep using email and password. They do not need an API key. Issue a separate key per product. Do not share one key across applications. Store each key on that application’s server only.',
        },
        { type: 'h2', text: 'Using the API from other applications' },
        {
          type: 'ul',
          items: [
            'Create one API client per application (for example “Mwalimu Backend”, “Chamaplus”, “Nova POS”).',
            'Copy the nova_live_ key into that app’s environment (NOVA_SMS_API_KEY). Never put it in a browser, mobile app, or Git.',
            'Call https://smsapi.novastack.co.ke from that app’s backend. The key selects the organization — do not send organizationId.',
            'Grant only the permissions that app needs (SMS_*, MPESA_*, WALLET_*).',
            'Safaricom STK and C2B callbacks stay on Nova SMS. Your app polls status or verifies a receipt. Do not register your own Daraja URL.',
            'Applications that share an organization also share the SMS wallet. Keys are not wallets.',
          ],
        },
        { type: 'h2', text: 'Base URL' },
        {
          type: 'p',
          text: 'Configured with NOVA_SMS_API_BASE_URL (no trailing slash). Paths are under /api/v1.',
        },
        { type: 'code', language: 'text', code: `${o}/api/v1` },
        { type: 'h2', text: 'Default provider and sender ID' },
        {
          type: 'p',
          text: 'Default provider: TalkSasa. If senderId is not supplied, the configured TalkSasa default sender ID is used. The current default is TALK-SASA (TALKSASA_SENDER_ID). Change the environment variable later without modifying application code. You never call TalkSasa directly — Nova SMS is the public API.',
        },
        { type: 'h2', text: 'Envelope' },
        {
          type: 'code',
          language: 'json',
          code: `{
  "success": true,
  "message": "OK",
  "data": {}
}`,
        },
        {
          type: 'note',
          text: 'There is no top-level error code field. Read HTTP status and message.',
        },
        { type: 'h2', text: 'Endpoints for API clients' },
        {
          type: 'table',
          headers: ['Method', 'Path', 'Permission'],
          rows: [
            ['POST', '/api/v1/sms/send', 'SMS_SEND'],
            ['POST', '/api/v1/sms/bulk', 'SMS_BULK'],
            ['GET', '/api/v1/sms/batches/{batchId}', 'SMS_STATUS'],
            ['POST', '/api/v1/sms/batches/{batchId}/resend-failed', 'SMS_BULK'],
            ['POST', '/api/v1/sms/{id}/resend', 'SMS_SEND'],
            ['POST', '/api/v1/sms/schedule', 'SMS_BULK'],
            ['GET', '/api/v1/sms/{id}', 'SMS_STATUS'],
            ['GET', '/api/v1/sms/{id}/status', 'SMS_STATUS'],
            ['GET', '/api/v1/sms', 'SMS_HISTORY'],
            ['GET', '/api/v1/sms/history', 'SMS_HISTORY'],
            ['GET', '/api/v1/wallet/balance', 'WALLET_READ'],
            ['GET', '/api/v1/wallet/transactions', 'WALLET_READ'],
            ['POST', '/api/v1/wallet/topup', 'WALLET_TOPUP'],
            ['GET', '/api/v1/wallet/topup/{id}', 'WALLET_TOPUP'],
            ['POST', '/api/v1/wallet/topup/{id}/check', 'WALLET_TOPUP'],
            ['POST', '/api/v1/wallet/topup/verify-receipt', 'WALLET_TOPUP'],
            ['POST', '/api/v1/mpesa/stkpush', 'MPESA_STK_PUSH'],
            ['POST', '/api/v1/mpesa/checkout', 'MPESA_STK_PUSH'],
            ['GET', '/api/v1/mpesa/transactions/{id}', 'MPESA_STATUS'],
            ['GET', '/api/v1/mpesa/transactions/{id}/status', 'MPESA_STATUS'],
            ['GET', '/api/v1/mpesa/c2b/transactions', 'MPESA_C2B'],
            ['GET', '/api/v1/mpesa/c2b/transactions/{id}', 'MPESA_C2B'],
          ],
        },
        { type: 'h2', text: 'Callbacks' },
        {
          type: 'p',
          text: 'Clients do not configure or implement Safaricom callbacks. Nova SMS handles Safaricom STK and C2B callbacks internally. Client applications initiate payments through the Nova SMS API and retrieve transaction status through the authenticated API.',
        },
        {
          type: 'ul',
          items: [
            'You do not implement Daraja callback structure, Safaricom credentials, STK parsing, C2B validation, receipt extraction, callback security, or reconciliation.',
            'C2B URL registration is Super Admin only — not an ordinary API client.',
            'Poll GET /api/v1/mpesa/transactions/{id}/status or list GET /api/v1/mpesa/c2b/transactions. A Nova webhook (payment.success, payment.failed, c2b.received) may be added later; polling remains the fallback.',
          ],
        },
        { type: 'h2', text: 'Security' },
        {
          type: 'warn',
          text: 'Never put the Nova SMS API key in frontend JavaScript, never commit it to Git, and never expose it to browser users. Call Nova SMS from your backend.',
        },
        {
          type: 'pre',
          text: `Browser
   ↓
Your Backend
   ↓
Nova SMS API`,
        },
        { type: 'h2', text: 'OpenAPI' },
        { type: 'p', text: `Swagger UI: ${o}/swagger-ui.html` },
        { type: 'p', text: `OpenAPI JSON: ${o}/v3/api-docs` },
      ],
    },
    'quick-start': {
      id: 'quick-start',
      title: 'Quick Start',
      description: 'Send your first SMS through Nova SMS in a few minutes.',
      blocks: [
        {
          type: 'ol',
          items: [
            'Create an API client (Developer → API Clients).',
            'Copy the nova_live_… key. It is shown only once.',
            'Store the key as NOVA_SMS_API_KEY (environment or secrets manager).',
            `Set the origin: export NOVA_SMS_API_URL="${o}"`,
            'POST /api/v1/sms/send from your backend.',
            'Read data.id and data.status from the JSON envelope.',
            'Optionally GET /api/v1/sms/{id}/status.',
          ],
        },
        { type: 'h2', text: 'First SMS' },
        {
          type: 'p',
          text: 'If senderId is not supplied, the configured TalkSasa default sender ID is used. The current default is TALK-SASA.',
        },
        {
          type: 'code',
          language: 'json',
          code: `{
  "recipient": "254712345678",
  "message": "Hello from Nova SMS."
}`,
        },
        { type: 'code', language: 'bash', code: helloCurl },
        {
          type: 'p',
          text: 'Success returns HTTP 200 with message "SMS queued". Delivery runs in the same request, so data.status is typically ACCEPTED, SENT, DELIVERED, or FAILED — not a TalkSasa payload.',
        },
      ],
    },
    authentication: {
      id: 'authentication',
      title: 'Authentication',
      description: 'API keys belong to API clients. Issue one client per application.',
      blocks: [
        { type: 'http', method: 'POST', path: 'X-API-Key: nova_live_xxxxxxxxxxxxxxxxx' },
        { type: 'code', language: 'http', code: 'X-API-Key: nova_live_xxxxxxxxxxxxxxxxx' },
        { type: 'code', language: 'bash', code: 'export NOVA_SMS_API_KEY="nova_live_xxxxxxxxx"' },
        { type: 'h2', text: 'One key per application' },
        {
          type: 'p',
          text: 'These client APIs are meant to be called from other products. Create a separate API client for each application. Mwalimu, Chamaplus, and POS must not share a key. If one app is compromised you revoke that client only.',
        },
        {
          type: 'ul',
          items: [
            'API keys are separate from dashboard passwords.',
            'Nova stores a SHA-256 hash. The full key is shown only at create or rotate.',
            'Revoked keys stop working immediately.',
            'The same organization can have many API clients. They share the organization wallet and SMS history.',
            'Idempotency-Key is stored per API client, so two apps can safely reuse the same business reference.',
            'Scoped live keys may call /api/v1/sms/**, /api/v1/wallet/**, and /api/v1/mpesa client routes when granted.',
            'Default permissions if none are selected: SMS_SEND, SMS_BULK, SMS_STATUS.',
            'Grant WALLET_READ and WALLET_TOPUP or MPESA_STK_PUSH so an app can show balance and start STK on its own site. WALLET_READ also covers C2B instructions.',
          ],
        },
        { type: 'h2', text: 'Create an API client' },
        {
          type: 'ol',
          items: [
            'Open Super Admin.',
            'Go to Developer.',
            'Open API Clients.',
            'Click New API client.',
            'Select the organization.',
            'Enter the application name (one client per product, for example Mwalimu Backend).',
            'Select permissions.',
            'Configure the rate limit (default 100/minute).',
            'Create the API client.',
            'Copy the API key.',
            'Store it securely.',
          ],
        },
        { type: 'h2', text: 'Invalid key' },
        {
          type: 'code',
          language: 'json',
          code: `{
  "success": false,
  "message": "Invalid API key."
}`,
        },
        {
          type: 'note',
          text: 'If the header is missing, HTTP 401 returns "Session expired. Please sign in again."',
        },
      ],
    },
    'send-sms': {
      id: 'send-sms',
      title: 'Send SMS',
      description: 'Send one SMS. Permission SMS_SEND.',
      blocks: [
        { type: 'http', method: 'POST', path: '/api/v1/sms/send' },
        {
          type: 'table',
          headers: ['Field', 'Required', 'Type', 'Description'],
          rows: [
            ['recipient', 'Yes', 'string (max 20)', '07…, 01…, 254…, or +254…'],
            ['message', 'Yes', 'string (max 1600)', 'SMS body, billed by segments'],
            ['senderId', 'No', 'string (max 11)', 'Optional. Approved org sender, or omit for TALK-SASA'],
          ],
        },
        { type: 'h2', text: 'Request' },
        {
          type: 'p',
          text: 'If senderId is not supplied, the configured TalkSasa default sender ID is used. The current default is TALK-SASA. Nova SMS remains the public API — do not call TalkSasa from your application.',
        },
        {
          type: 'code',
          language: 'json',
          code: `{
  "recipient": "254712345678",
  "message": "Your payment has been received."
}`,
        },
        { type: 'h2', text: 'cURL' },
        { type: 'code', language: 'bash', code: sendCurl },
        { type: 'h3', text: 'With sender ID' },
        { type: 'code', language: 'bash', code: sendSenderCurl },
        { type: 'h2', text: 'Response' },
        {
          type: 'p',
          text: 'HTTP 200. Envelope message is "SMS queued". id and messageId are the same Nova UUID. providerMessageId is the TalkSasa uid once accepted.',
        },
        {
          type: 'code',
          language: 'json',
          code: `{
  "success": true,
  "message": "SMS queued",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "messageId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "providerMessageId": "606812e63f78b",
    "recipient": "254712345678",
    "status": "ACCEPTED",
    "smsUnits": 1,
    "cost": 1.00,
    "encoding": "GSM7",
    "channel": "SMS"
  }
}`,
        },
        { type: 'h2', text: 'Resend a failed SMS' },
        { type: 'http', method: 'POST', path: '/api/v1/sms/{id}/resend' },
        {
          type: 'p',
          text: 'Nova does not retry FAILED messages automatically. POST /sms/{id}/resend creates a new message. Use a new Idempotency-Key. For batches, see Retry failed SMS.',
        },
      ],
    },
    'bulk-sms': {
      id: 'bulk-sms',
      title: 'Bulk SMS',
      description: 'Send to many numbers and/or a contact group. Permission SMS_BULK.',
      blocks: [
        { type: 'http', method: 'POST', path: '/api/v1/sms/bulk' },
        {
          type: 'table',
          headers: ['Field', 'Required', 'Type', 'Description'],
          rows: [
            ['recipients', 'Conditional', 'string[]', 'Provide recipients, groupId, or both'],
            ['message', 'Yes', 'string (max 1600)', 'SMS body'],
            ['senderId', 'No', 'string (max 11)', 'Approved sender ID'],
            ['groupId', 'Conditional', 'UUID', 'Nova contact group in this organization'],
          ],
        },
        {
          type: 'note',
          text: 'There is no API maximum recipient count. Provider calls are chunked with SMS_BATCH_SIZE (default 100).',
        },
        {
          type: 'code',
          language: 'json',
          code: `{
  "recipients": ["254712345678", "254701234567", "254711234567"],
  "message": "Important announcement."
}`,
        },
        {
          type: 'p',
          text: 'Delivery runs in the same request. data.status on the batch wrapper is always the string "PROCESSING". Check data.messages[].status for each recipient.',
        },
        {
          type: 'code',
          language: 'json',
          code: `{
  "success": true,
  "message": "Bulk SMS queued",
  "data": {
    "batchId": "b0b0b0b0-1111-2222-3333-444444444444",
    "queuedCount": 3,
    "recipientCount": 3,
    "smsUnits": 3,
    "status": "PROCESSING",
    "messages": []
  }
}`,
        },
        { type: 'h2', text: 'Resend failed recipients' },
        {
          type: 'p',
          text: 'FAILED messages are not retried automatically. Inspect the batch with GET /api/v1/sms/batches/{batchId}, then POST /api/v1/sms/batches/{batchId}/resend-failed. That creates a new batch of failed recipients only. Full client guide: Retry failed SMS.',
        },
      ],
    },
    'retry-failed': {
      id: 'retry-failed',
      title: 'Retry failed SMS',
      description: 'How to resend FAILED messages. Nova does not retry them automatically.',
      blocks: [
        {
          type: 'p',
          text: 'A FAILED, REJECTED, or CANCELLED message is finished. Nova does not queue it again. Submit a new send. Original rows stay in history so you keep an audit trail.',
        },
        {
          type: 'warn',
          text: 'Do not reuse the original Idempotency-Key. A new send or resend needs a new key. Reusing the bulk key replays the original batch, including numbers that already SENT.',
        },
        { type: 'h2', text: 'Two kinds of failure' },
        {
          type: 'table',
          headers: ['What happened', 'HTTP', 'Wallet', 'What you do'],
          rows: [
            [
              'Organization Nova SMS wallet too low',
              '402',
              'Nothing deducted. Nothing queued.',
              'Top up the org wallet. POST /sms/send or /sms/bulk again with a new Idempotency-Key.',
            ],
            [
              'Provider rejected the send (including no remaining units)',
              '200 with data.status FAILED',
              'Debited then refunded',
              'After the provider is funded, resend. See endpoints below.',
            ],
          ],
        },
        {
          type: 'p',
          text: 'HTTP 402 envelope: message "Insufficient wallet balance". data includes required, available, and currency. Do not treat this as a stored FAILED SMS.',
        },
        {
          type: 'p',
          text: 'Provider unit failures often set failureReason to "SMS provider has no remaining units. Please contact support." Contact Nova SMS support. Then resend.',
        },
        { type: 'h2', text: 'Inspect a batch' },
        { type: 'http', method: 'GET', path: '/api/v1/sms/batches/{batchId}' },
        {
          type: 'p',
          text: 'Permission SMS_STATUS. Returns every recipient in that batch. data.failedCount is how many are FAILED, REJECTED, or CANCELLED. Read data.messages[].status. SENT, DELIVERED, ACCEPTED, and PENDING must not be sent again.',
        },
        {
          type: 'code',
          language: 'bash',
          code: `curl -X GET "${o}/api/v1/sms/batches/\${BATCH_ID}" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Accept: application/json"`,
        },
        {
          type: 'code',
          language: 'json',
          code: `{
  "success": true,
  "message": "OK",
  "data": {
    "batchId": "b0b0b0b0-1111-2222-3333-444444444444",
    "queuedCount": 100,
    "recipientCount": 100,
    "failedCount": 7,
    "status": "COMPLETED",
    "messages": []
  }
}`,
        },
        { type: 'h2', text: 'Resend failed recipients in a batch' },
        { type: 'http', method: 'POST', path: '/api/v1/sms/batches/{batchId}/resend-failed' },
        {
          type: 'p',
          text: 'Permission SMS_BULK. Creates a new batch for failed recipients only. SENT and DELIVERED numbers are skipped. Wallet is checked again (HTTP 402 if still short). Original FAILED rows are not changed to PENDING.',
        },
        {
          type: 'code',
          language: 'bash',
          code: `curl -X POST "${o}/api/v1/sms/batches/\${BATCH_ID}/resend-failed" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Idempotency-Key: resend-\${BATCH_ID}-\$(date +%s)" \\
  -H "Accept: application/json"`,
        },
        {
          type: 'code',
          language: 'json',
          code: `{
  "success": true,
  "message": "Failed SMS resent",
  "data": {
    "batchId": "c1c1c1c1-1111-2222-3333-555555555555",
    "sourceBatchId": "b0b0b0b0-1111-2222-3333-444444444444",
    "queuedCount": 7,
    "resentCount": 7,
    "skippedCount": 93,
    "status": "PROCESSING",
    "messages": []
  }
}`,
        },
        {
          type: 'note',
          text: 'If 100 were sent and 7 failed, this sends 7, not 100. Attempt 1 stays FAILED and refunded. Attempt 2 is a new charge if the provider accepts it.',
        },
        { type: 'h2', text: 'Resend one failed SMS' },
        { type: 'http', method: 'POST', path: '/api/v1/sms/{id}/resend' },
        {
          type: 'p',
          text: 'Permission SMS_SEND. {id} is data.id from the original send. Only FAILED, REJECTED, or CANCELLED messages can be resent. Creates a new message for the same recipient and copy. HTTP 400 if the message is not failed.',
        },
        {
          type: 'code',
          language: 'bash',
          code: `curl -X POST "${o}/api/v1/sms/\${MESSAGE_ID}/resend" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Idempotency-Key: resend-\${MESSAGE_ID}-\$(date +%s)" \\
  -H "Accept: application/json"`,
        },
        {
          type: 'table',
          headers: ['HTTP', 'When'],
          rows: [
            ['200', 'New SMS created and sent'],
            ['400', 'No failed recipients, or the message is not FAILED/REJECTED/CANCELLED'],
            ['402', 'Organization wallet cannot cover the new send'],
            ['404', 'Unknown batch or message id for this organization'],
          ],
        },
      ],
    },
    status: {
      id: 'status',
      title: 'SMS Status',
      description: 'Look up a message by Nova SMS UUID or TalkSasa uid. Permission SMS_STATUS.',
      blocks: [
        { type: 'http', method: 'GET', path: '/api/v1/sms/{id}' },
        { type: 'http', method: 'GET', path: '/api/v1/sms/{id}/status' },
        {
          type: 'p',
          text: '{id} is data.id from send, or data.providerMessageId (TalkSasa uid). GET /api/v1/sms/{id} returns the stored row for this organization only. GET .../status refreshes from TalkSasa GET /sms/{uid}. Other organizations’ uids return 404.',
        },
        { type: 'h2', text: 'Statuses' },
        {
          type: 'pre',
          text: `PENDING → ACCEPTED / SENT / DELIVERED
SCHEDULED (until dispatch)
FAILED / REJECTED / CANCELLED (refunded)`,
        },
        {
          type: 'p',
          text: 'FAILED is terminal. Nova does not auto-retry it. Resend with POST /api/v1/sms/{id}/resend or POST /api/v1/sms/batches/{batchId}/resend-failed. See Retry failed SMS.',
        },
        {
          type: 'p',
          text: 'Enum also includes QUEUED and PROCESSING. The current send path saves PENDING, then maps the provider result.',
        },
        {
          type: 'code',
          language: 'bash',
          code: `curl -X GET "${o}/api/v1/sms/\${MESSAGE_ID}/status" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Accept: application/json"`,
        },
      ],
    },
    history: {
      id: 'history',
      title: 'SMS History',
      description: 'Paged history for the API client’s organization. Permission SMS_HISTORY.',
      blocks: [
        { type: 'http', method: 'GET', path: '/api/v1/sms?page=0&size=50' },
        { type: 'http', method: 'GET', path: '/api/v1/sms/history?page=0&size=50' },
        {
          type: 'p',
          text: 'GET /api/v1/sms and GET /api/v1/sms/history are the same org-scoped list. Default size is 20. data is a Spring Page (content, totalElements, totalPages, size, number). This is not TalkSasa’s shared inbox; clients cannot read other organizations.',
        },
        {
          type: 'code',
          language: 'bash',
          code: `curl -X GET "${o}/api/v1/sms/history?page=0&size=50" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Accept: application/json"`,
        },
      ],
    },
    wallet: {
      id: 'wallet',
      title: 'Wallet',
      description:
        'Show organization balance and accept M-Pesa STK top-ups from your own site. Permission WALLET_READ / WALLET_TOPUP.',
      blocks: [
        {
          type: 'p',
          text: 'Partner apps (Mwalimu, Chamaplus, Nova POS, and others) can display SMS credit and let users fund the organization wallet without signing into the Nova SMS portal. POST /api/v1/wallet/topup internally uses POST /api/v1/mpesa/stkpush. Nova handles the Safaricom callback; poll the returned transaction id. Never put the live key in browser JavaScript.',
        },
        {
          type: 'pre',
          text: `User on your site
   ↓
Your backend
   ↓
Nova SMS Wallet Top-up (POST /api/v1/wallet/topup)
   ↓
M-Pesa STK (same as POST /api/v1/mpesa/stkpush)
   ↓
Safaricom
   ↓
Nova SMS callback (not your server)
   ↓
Wallet credited`,
        },
        { type: 'h2', text: 'Permissions' },
        {
          type: 'ul',
          items: [
            'WALLET_READ — GET /api/v1/wallet/balance and GET /api/v1/wallet/transactions.',
            'WALLET_TOPUP — POST /api/v1/wallet/topup, GET /api/v1/wallet/topup/{id}, POST /api/v1/wallet/topup/{id}/check.',
            'Enable them on the API client (Developer → API Clients → Permissions). Existing keys can be updated without rotating.',
            'The wallet is the organization wallet. All API clients for that org share the same balance.',
          ],
        },
        { type: 'h2', text: 'Balance' },
        { type: 'http', method: 'GET', path: '/api/v1/wallet/balance' },
        {
          type: 'code',
          language: 'bash',
          code: `curl -X GET "${o}/api/v1/wallet/balance" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Accept: application/json"`,
        },
        {
          type: 'code',
          language: 'json',
          code: `{
  "success": true,
  "message": "OK",
  "data": {
    "walletId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "organizationId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "balance": 1500.00,
    "currency": "KES",
    "smsCost": 1.00,
    "availableSms": 1500
  }
}`,
        },
        { type: 'h2', text: 'Top up (M-Pesa STK)' },
        { type: 'http', method: 'POST', path: '/api/v1/wallet/topup' },
        {
          type: 'table',
          headers: ['Field', 'Required', 'Type', 'Description'],
          rows: [
            ['amount', 'Yes', 'number', 'KES. Minimum 1.00'],
            ['phoneNumber', 'Yes', 'string', 'M-Pesa phone that receives the STK prompt. 07…, 254…, or +254…'],
          ],
        },
        {
          type: 'code',
          language: 'bash',
          code: `curl -X POST "${o}/api/v1/wallet/topup" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Content-Type: application/json" \\
  -H "Accept: application/json" \\
  -d '{
    "amount": 500,
    "phoneNumber": "254712345678"
  }'`,
        },
        {
          type: 'p',
          text: 'Nova SMS is the source of truth. Do not mark the payment successful from the M-Pesa PIN screen. Read data.transactionId, then poll Nova SMS until status is COMPLETED and walletCredited is true.',
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
        { type: 'h3', text: 'Polling rules' },
        {
          type: 'ol',
          items: [
            'Read data.transactionId from the initial top-up response.',
            'Wait approximately 5 seconds before the first status check.',
            'Call POST /api/v1/wallet/topup/{transactionId}/check every 3–5 seconds.',
            'Continue polling while status is PENDING.',
            '"The transaction is still under processing" is PENDING, not FAILED.',
            'Stop polling when status is COMPLETED and walletCredited=true.',
            'Stop polling when Nova reports a definitive FAILED status.',
            'Never create another STK Push while the existing transaction is still PENDING.',
            'GET /api/v1/wallet/topup/{id} may be used to recover/read the transaction, but it does not query Safaricom.',
            'After successful completion, call GET /api/v1/wallet/balance to refresh the organization\'s SMS balance.',
          ],
        },
        { type: 'http', method: 'POST', path: '/api/v1/wallet/topup/{transactionId}/check' },
        { type: 'http', method: 'GET', path: '/api/v1/wallet/topup/{transactionId}' },
        {
          type: 'p',
          text: 'POST …/check reads the database and, if still PENDING, queries Safaricom and updates the row. GET …/{id} only reads the stored row.',
        },
        {
          type: 'code',
          language: 'bash',
          code: `curl -X POST "${o}/api/v1/wallet/topup/\${TRANSACTION_ID}/check" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Accept: application/json"`,
        },
        {
          type: 'h3',
          text: 'Statuses',
        },
        {
          type: 'table',
          headers: ['status', 'Meaning', 'What your app should do'],
          rows: [
            ['PENDING', 'STK sent, or Safaricom still processing (including “The transaction is still under processing”). Not a failure.', 'Keep polling. Do not treat this as FAILED.'],
            ['COMPLETED', 'Payment succeeded. Wallet credited once.', 'Stop polling only when walletCredited is also true. Then GET /api/v1/wallet/balance. Use mpesaReceipt.'],
            ['FAILED', 'Definitive failure: user cancelled, insufficient funds, wrong PIN, or similar.', 'Stop polling. Show failure. Do not credit the UI.'],
          ],
        },
        {
          type: 'table',
          headers: ['Field', 'Meaning'],
          rows: [
            ['walletCredited', 'true after Nova SMS has credited the organization wallet exactly once. This is the success flag.'],
            ['callbackReceived', 'true after Safaricom’s STK callback was applied. A successful STK query can credit the wallet before the callback arrives, so this can still be false when walletCredited is true.'],
            ['mpesaReceipt', 'M-Pesa receipt (for example UHJA53YW7O) once Safaricom confirms.'],
            ['resultDesc', 'Latest Safaricom description. “Still under processing” means PENDING, not FAILED.'],
          ],
        },
        {
          type: 'note',
          text: 'Status never moves backwards. COMPLETED is never overwritten with PENDING or FAILED. Keep polling PENDING; Nova SMS applies the Safaricom callback even if an earlier check was still processing.',
        },
        {
          type: 'code',
          language: 'json',
          code: `{
  "success": true,
  "message": "OK",
  "data": {
    "transactionId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "checkoutRequestId": "ws_CO_19082026104512345",
    "status": "COMPLETED",
    "amount": 500.00,
    "phoneNumber": "254712345678",
    "mpesaReceipt": "UHJA53YW7O",
    "callbackReceived": true,
    "walletCredited": true
  }
}`,
        },
        {
          type: 'code',
          language: 'json',
          code: `{
  "success": true,
  "message": "Still pending — waiting for Safaricom callback or user PIN entry",
  "data": {
    "transactionId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "status": "PENDING",
    "amount": 500.00,
    "phoneNumber": "254712345678",
    "resultDesc": "The transaction is still under processing",
    "callbackReceived": false,
    "walletCredited": false
  }
}`,
        },
        { type: 'h2', text: 'Transactions' },
        { type: 'http', method: 'GET', path: '/api/v1/wallet/transactions?page=0&size=20' },
        {
          type: 'p',
          text: 'Optional filters: type=TOPUP|SMS_DEBIT|REFUND|ADJUSTMENT and status=PENDING|COMPLETED|FAILED. data is a Spring Page.',
        },
        {
          type: 'warn',
          text: 'Never call Nova SMS from the browser with a live key. Your site UI should call your backend; your backend calls Nova SMS.',
        },
      ],
    },
    errors: {
      id: 'errors',
      title: 'Errors',
      description: 'Actual HTTP statuses and messages returned by Nova SMS.',
      blocks: [
        {
          type: 'p',
          text: 'Envelope: { "success": false, "message": "..." }. No code field.',
        },
        {
          type: 'table',
          headers: ['HTTP', 'When', 'message'],
          rows: [
            ['401', 'Missing key / no JWT', 'Session expired. Please sign in again.'],
            ['401', 'Bad, revoked, or expired key', 'Invalid API key.'],
            ['402', 'Wallet too low before send', 'Insufficient wallet balance'],
            ['400', 'Resend of a non-failed message', 'Only failed messages can be resent'],
            ['400', 'Batch resend with nothing failed', 'No failed messages to resend'],
            ['403', 'Scoped key off /sms, /wallet, and client /mpesa', 'This API key cannot access that resource'],
            ['403', 'Missing permission', 'API key is missing permission SMS_HISTORY'],
            ['400', 'Invalid phone', "Invalid phone number '…'. Use 07…, 01…, 254…, or +254…"],
            ['409', 'Idempotency body mismatch', 'Idempotency-Key was reused with a different request body'],
            ['429', 'Client rate limit', 'Too many API requests. Please wait a minute and try again.'],
            ['404', 'Unknown SMS id', 'SMS message not found'],
            ['502', 'Uncaught provider HTTP error', 'SMS provider is temporarily unavailable. Please try again.'],
          ],
        },
        {
          type: 'note',
          text: 'Invalid phones return 400, not 422. HTTP 402 means nothing was queued or deducted — top up, then send again. Provider send failures often return HTTP 200 with data.status FAILED, a refund, and failureReason. Those are not retried automatically. See Retry failed SMS.',
        },
      ],
    },
    idempotency: {
      id: 'idempotency',
      title: 'Idempotency',
      description: 'Prevent duplicate SMS on retries (payments, OTPs, callbacks).',
      blocks: [
        { type: 'p', text: 'Send Idempotency-Key on POST /sms/send, POST /sms/bulk, POST /sms/batches/{batchId}/resend-failed, POST /mpesa/stkpush, POST /mpesa/checkout, and POST /wallet/topup. Example for payments: Idempotency-Key: learner-839-payment-2026-08-24 — a timeout retry returns the existing STK instead of a second prompt.' },
        {
          type: 'code',
          language: 'bash',
          code: `curl -X POST "${o}/api/v1/sms/send" \\
  -H "X-API-Key: \${NOVA_SMS_API_KEY}" \\
  -H "Idempotency-Key: payment-123456" \\
  -H "Content-Type: application/json" \\
  -d '{
    "recipient": "254712345678",
    "message": "Payment received."
  }'`,
        },
        {
          type: 'ul',
          items: [
            'Same key + same body → original SMS is returned.',
            'Same key + different body → HTTP 409.',
            'JWT dashboard users do not use this store.',
          ],
        },
      ],
    },
    'rate-limits': {
      id: 'rate-limits',
      title: 'Rate Limits',
      description: 'Per API client, configurable by Super Admin.',
      blocks: [
        {
          type: 'ul',
          items: [
            'Default for a new client: 100 requests/minute.',
            'Configurable range: 1–10,000 per minute.',
            'HTTP 429 when exceeded.',
            'This is not a universal platform quota.',
          ],
        },
        {
          type: 'code',
          language: 'json',
          code: `{
  "success": false,
  "message": "Too many API requests. Please wait a minute and try again."
}`,
        },
      ],
    },
    'spring-boot': {
      id: 'spring-boot',
      title: 'Spring Boot',
      description: 'Call Nova SMS and M-Pesa from another application’s backend with WebClient. One API key per app.',
      blocks: [
        {
          type: 'p',
          text: 'Run this in Mwalimu, Chamaplus, POS, or any other server — not in the browser. Give that application its own nova_live_ key.',
        },
        {
          type: 'code',
          language: 'yaml',
          code: `nova:
  sms:
    base-url: \${NOVA_SMS_API_URL}
    api-key: \${NOVA_SMS_API_KEY}`,
        },
        {
          type: 'code',
          language: 'java',
          code: `@Bean
WebClient novaSmsWebClient(
        @Value("\${nova.sms.base-url}") String baseUrl,
        @Value("\${nova.sms.api-key}") String apiKey) {
    return WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("X-API-Key", apiKey)
            .defaultHeader("Accept", "application/json")
            .build();
}`,
        },
        { type: 'h2', text: 'Send SMS' },
        {
          type: 'code',
          language: 'java',
          code: `return webClient.post()
        .uri("/api/v1/sms/send")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Idempotency-Key", orderId)
        .bodyValue(Map.of("recipient", recipient, "message", message))
        .retrieve()
        .bodyToMono(SmsEnvelope.class);`,
        },
        { type: 'h2', text: 'STK Push / checkout' },
        {
          type: 'code',
          language: 'java',
          code: `return webClient.post()
        .uri("/api/v1/mpesa/stkpush")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Idempotency-Key", orderId)
        .bodyValue(Map.of("amount", amount, "phoneNumber", phone))
        .retrieve()
        .bodyToMono(StkEnvelope.class);

// Poll until COMPLETED and walletCredited == true (or FAILED).
return webClient.get()
        .uri("/api/v1/mpesa/transactions/{id}/status", transactionId)
        .retrieve()
        .bodyToMono(StkEnvelope.class);`,
        },
        { type: 'h2', text: 'Paybill C2B' },
        {
          type: 'p',
          text: 'GET /api/v1/mpesa/c2b for paybill and account. After the customer pays, POST /api/v1/mpesa/c2b/verify with the receipt. Do not handle the Safaricom callback in this application.',
        },
      ],
    },
    nodejs: {
      id: 'nodejs',
      title: 'Node.js',
      description: 'fetch() from a Node server for this application — never from the browser.',
      blocks: [
        {
          type: 'p',
          text: 'Each product should use its own NOVA_SMS_API_KEY. Call Nova from this app’s backend only.',
        },
        { type: 'h2', text: 'Send SMS' },
        {
          type: 'code',
          language: 'javascript',
          code: `const response = await fetch(
  \`\${process.env.NOVA_SMS_API_URL}/api/v1/sms/send\`,
  {
    method: "POST",
    headers: {
      "X-API-Key": process.env.NOVA_SMS_API_KEY,
      "Idempotency-Key": orderId,
      "Content-Type": "application/json",
      "Accept": "application/json"
    },
    body: JSON.stringify({
      recipient: "254712345678",
      message: "Payment received."
    })
  }
);
const data = await response.json();`,
        },
        { type: 'h2', text: 'STK Push' },
        {
          type: 'code',
          language: 'javascript',
          code: `const started = await fetch(
  \`\${process.env.NOVA_SMS_API_URL}/api/v1/mpesa/stkpush\`,
  {
    method: "POST",
    headers: {
      "X-API-Key": process.env.NOVA_SMS_API_KEY,
      "Idempotency-Key": orderId,
      "Content-Type": "application/json",
      "Accept": "application/json"
    },
    body: JSON.stringify({ amount: 500, phoneNumber: "254712345678" })
  }
);
const { data } = await started.json();
// Poll GET /api/v1/mpesa/transactions/\${data.transactionId}/status
// until status is COMPLETED and walletCredited is true, or FAILED.`,
        },
      ],
    },
    php: {
      id: 'php',
      title: 'PHP',
      description: 'cURL from this application’s PHP backend.',
      blocks: [
        {
          type: 'p',
          text: 'Store NOVA_SMS_API_KEY in this app’s environment. Do not reuse another product’s key.',
        },
        { type: 'h2', text: 'Send SMS' },
        {
          type: 'code',
          language: 'php',
          code: `$ch = curl_init(getenv('NOVA_SMS_API_URL') . '/api/v1/sms/send');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST => true,
    CURLOPT_HTTPHEADER => [
        'X-API-Key: ' . getenv('NOVA_SMS_API_KEY'),
        'Idempotency-Key: ' . $orderId,
        'Content-Type: application/json',
        'Accept: application/json',
    ],
    CURLOPT_POSTFIELDS => json_encode([
        'recipient' => '254712345678',
        'message' => 'Payment received.',
    ]),
]);
$body = curl_exec($ch);
$result = json_decode($body, true);`,
        },
        { type: 'h2', text: 'STK Push' },
        {
          type: 'code',
          language: 'php',
          code: `$ch = curl_init(getenv('NOVA_SMS_API_URL') . '/api/v1/mpesa/stkpush');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST => true,
    CURLOPT_HTTPHEADER => [
        'X-API-Key: ' . getenv('NOVA_SMS_API_KEY'),
        'Idempotency-Key: ' . $orderId,
        'Content-Type: application/json',
        'Accept: application/json',
    ],
    CURLOPT_POSTFIELDS => json_encode([
        'amount' => 500,
        'phoneNumber' => '254712345678',
    ]),
]);
$stk = json_decode(curl_exec($ch), true);
// Poll GET /api/v1/mpesa/transactions/{transactionId}/status`,
        },
      ],
    },
    python: {
      id: 'python',
      title: 'Python',
      description: 'requests from this application’s Python backend.',
      blocks: [
        {
          type: 'p',
          text: 'Each integrating app should load its own NOVA_SMS_API_KEY from the environment.',
        },
        { type: 'h2', text: 'Send SMS' },
        {
          type: 'code',
          language: 'python',
          code: `import os
import requests

headers = {
    "X-API-Key": os.environ["NOVA_SMS_API_KEY"],
    "Idempotency-Key": order_id,
    "Content-Type": "application/json",
    "Accept": "application/json",
}
base = os.environ["NOVA_SMS_API_URL"]

response = requests.post(
    f"{base}/api/v1/sms/send",
    headers=headers,
    json={"recipient": "254712345678", "message": "Payment received."},
)
print(response.json())`,
        },
        { type: 'h2', text: 'STK Push' },
        {
          type: 'code',
          language: 'python',
          code: `stk = requests.post(
    f"{base}/api/v1/mpesa/stkpush",
    headers=headers,
    json={"amount": 500, "phoneNumber": "254712345678"},
).json()
tx_id = stk["data"]["transactionId"]
status = requests.get(
    f"{base}/api/v1/mpesa/transactions/{tx_id}/status",
    headers={"X-API-Key": os.environ["NOVA_SMS_API_KEY"], "Accept": "application/json"},
).json()
# Stop when status is COMPLETED and walletCredited is true, or FAILED.`,
        },
      ],
    },
    'generic-http': {
      id: 'generic-http',
      title: 'Generic HTTP',
      description: 'Any HTTP client in any application can integrate. No SDK required.',
      blocks: [
        {
          type: 'p',
          text: 'Use the same origin and X-API-Key from Mwalimu, Chamaplus, POS, or any other backend. One key per application.',
        },
        { type: 'http', method: 'POST', path: '/api/v1/sms/send' },
        { type: 'http', method: 'POST', path: '/api/v1/mpesa/stkpush' },
        { type: 'http', method: 'POST', path: '/api/v1/mpesa/checkout' },
        { type: 'http', method: 'GET', path: '/api/v1/mpesa/transactions/{id}/status' },
        { type: 'http', method: 'GET', path: '/api/v1/mpesa/c2b/transactions' },
        { type: 'http', method: 'GET', path: '/api/v1/mpesa/c2b/transactions/{id}' },
        { type: 'p', text: 'Header: X-API-Key. Content-Type: application/json. Optional Idempotency-Key on send and STK/checkout.' },
        {
          type: 'warn',
          text: 'Never call Nova SMS from browser JavaScript with a live key. Browser or mobile app → your backend → Nova SMS. Safaricom callbacks terminate on Nova, not on your application.',
        },
      ],
    },
    provider: {
      id: 'provider',
      title: 'TalkSasa (internal)',
      description: 'INTERNAL ONLY. Integrators do not need a TalkSasa account.',
      blocks: [
        { type: 'warn', text: 'INTERNAL ONLY. Do not share TalkSasa tokens. Public docs must not require a TalkSasa account.' },
        {
          type: 'pre',
          text: `Nova SMS
   ↓
TalkSasaSmsProvider
   ↓
TalkSasa Bulk SMS API v3`,
        },
        {
          type: 'table',
          headers: ['Item', 'Value'],
          rows: [
            ['Base URL', 'TALKSASA_BASE_URL (default https://bulksms.talksasa.com/api/v3)'],
            ['Auth', 'Bearer TALKSASA_API_TOKEN (server env only)'],
            ['Send', 'POST /sms/send'],
            ['View one', 'GET /sms/{uid} (queue uid uses GET /sms/queue/{uid} first)'],
            ['View all', 'GET /sms (Super Admin GET /api/v1/admin/talksasa/sms)'],
            ['Status', 'GET /sms/{uid}'],
            ['Default sender', 'TALKSASA_SENDER_ID (default TALK-SASA), configurable'],
            ['Retries', 'Up to novastack.sms.max-retries (default 3) on 408/502/503/504'],
            ['Batch chunk', 'SMS_BATCH_SIZE default 100'],
          ],
        },
        {
          type: 'p',
          text: 'The public API does not change if another provider is added. Super Admin can inspect GET /api/v1/admin/talksasa, GET /api/v1/admin/talksasa/sms, and GET /api/v1/admin/talksasa/sms/{uid}. Organization API keys never receive the TalkSasa inbox.',
        },
      ],
    },
  }
}
