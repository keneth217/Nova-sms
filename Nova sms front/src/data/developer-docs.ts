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

export const developerNav: DocNavGroup[] = [
  {
    label: 'Getting started',
    items: [
      { id: 'overview', label: 'Overview', to: '/admin/developer' },
      { id: 'quick-start', label: 'Quick Start', to: '/admin/developer/quick-start' },
      { id: 'authentication', label: 'Authentication', to: '/admin/developer/authentication' },
    ],
  },
  {
    label: 'API reference',
    items: [
      { id: 'send-sms', label: 'Send SMS', to: '/admin/developer/send-sms' },
      { id: 'bulk-sms', label: 'Bulk SMS', to: '/admin/developer/bulk-sms' },
      { id: 'status', label: 'SMS Status', to: '/admin/developer/status' },
      { id: 'history', label: 'SMS History', to: '/admin/developer/history' },
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
    label: 'Manage',
    items: [
      { id: 'clients', label: 'API Clients', to: '/admin/developer/clients' },
      { id: 'usage', label: 'API Usage', to: '/admin/developer/usage' },
      { id: 'console', label: 'Test Console', to: '/admin/developer/console' },
    ],
  },
  {
    label: 'Internal',
    items: [{ id: 'provider', label: 'TalkSasa (internal)', to: '/admin/developer/provider' }],
  },
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
    overview: {
      id: 'overview',
      title: 'API Overview',
      description: 'Nova SMS REST API for integrating applications.',
      blocks: [
        {
          type: 'p',
          text: 'Nova SMS provides a REST API that allows external applications to send SMS through Nova SMS without communicating directly with the underlying SMS provider.',
        },
        {
          type: 'p',
          text: 'Use this API from Mwalimu Scheme, Chamaplus, Nova POS, SACCO and school systems, other NovaStack apps, and third-party backends. Dashboard users keep using email and password. They do not need an API key.',
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
            ['POST', '/api/v1/sms/schedule', 'SMS_BULK'],
            ['GET', '/api/v1/sms/{id}', 'SMS_STATUS'],
            ['GET', '/api/v1/sms/{id}/status', 'SMS_STATUS'],
            ['GET', '/api/v1/sms/history', 'SMS_HISTORY'],
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
      description: 'API keys belong to API clients. API clients belong to organizations.',
      blocks: [
        { type: 'http', method: 'POST', path: 'X-API-Key: nova_live_xxxxxxxxxxxxxxxxx' },
        { type: 'code', language: 'http', code: 'X-API-Key: nova_live_xxxxxxxxxxxxxxxxx' },
        { type: 'code', language: 'bash', code: 'export NOVA_SMS_API_KEY="nova_live_xxxxxxxxx"' },
        { type: 'h2', text: 'Rules' },
        {
          type: 'ul',
          items: [
            'API keys are separate from dashboard passwords.',
            'Nova stores a SHA-256 hash. The full key is shown only at create or rotate.',
            'Revoked keys stop working immediately.',
            'Scoped live keys may only call /api/v1/sms/**.',
            'Default permissions if none are selected: SMS_SEND, SMS_BULK, SMS_STATUS.',
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
            'Enter the application name.',
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
          text: 'HTTP 200. Envelope message is "SMS queued". id and messageId are the same UUID.',
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
    "recipient": "254712345678",
    "status": "ACCEPTED",
    "smsUnits": 1,
    "cost": 1.00,
    "encoding": "GSM7",
    "channel": "SMS"
  }
}`,
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
      ],
    },
    status: {
      id: 'status',
      title: 'SMS Status',
      description: 'Look up a message by Nova SMS UUID. Permission SMS_STATUS.',
      blocks: [
        { type: 'http', method: 'GET', path: '/api/v1/sms/{id}/status' },
        {
          type: 'p',
          text: '{id} is a UUID (data.id from send). GET /api/v1/sms/{id} returns the stored row. GET .../status refreshes from the provider.',
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
        { type: 'http', method: 'GET', path: '/api/v1/sms/history?page=0&size=50' },
        {
          type: 'p',
          text: 'Default size is 20. data is a Spring Page (content, totalElements, totalPages, size, number). Clients cannot read other organizations.',
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
            ['402', 'Wallet too low', 'Insufficient wallet balance'],
            ['403', 'Scoped key off /sms', 'This API key cannot access that resource'],
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
          text: 'Invalid phones return 400, not 422. Provider send failures often return HTTP 200 with data.status FAILED and a refund.',
        },
      ],
    },
    idempotency: {
      id: 'idempotency',
      title: 'Idempotency',
      description: 'Prevent duplicate SMS on retries (payments, OTPs, callbacks).',
      blocks: [
        { type: 'p', text: 'Send Idempotency-Key on POST /sms/send and POST /sms/bulk for scoped API clients.' },
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
      description: 'Call Nova SMS with WebClient from your backend.',
      blocks: [
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
        {
          type: 'code',
          language: 'java',
          code: `return webClient.post()
        .uri("/api/v1/sms/send")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("recipient", recipient, "message", message))
        .retrieve()
        .bodyToMono(SmsEnvelope.class);`,
        },
      ],
    },
    nodejs: {
      id: 'nodejs',
      title: 'Node.js',
      description: 'fetch() from a Node server — never from the browser.',
      blocks: [
        {
          type: 'code',
          language: 'javascript',
          code: `const response = await fetch(
  \`\${process.env.NOVA_SMS_API_URL}/api/v1/sms/send\`,
  {
    method: "POST",
    headers: {
      "X-API-Key": process.env.NOVA_SMS_API_KEY,
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
      ],
    },
    php: {
      id: 'php',
      title: 'PHP',
      description: 'cURL from PHP on the server.',
      blocks: [
        {
          type: 'code',
          language: 'php',
          code: `$ch = curl_init(getenv('NOVA_SMS_API_URL') . '/api/v1/sms/send');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST => true,
    CURLOPT_HTTPHEADER => [
        'X-API-Key: ' . getenv('NOVA_SMS_API_KEY'),
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
      ],
    },
    python: {
      id: 'python',
      title: 'Python',
      description: 'requests from a Python backend.',
      blocks: [
        {
          type: 'code',
          language: 'python',
          code: `import os
import requests

response = requests.post(
    f"{os.environ['NOVA_SMS_API_URL']}/api/v1/sms/send",
    headers={
        "X-API-Key": os.environ["NOVA_SMS_API_KEY"],
        "Content-Type": "application/json",
        "Accept": "application/json"
    },
    json={
        "recipient": "254712345678",
        "message": "Payment received."
    }
)
print(response.json())`,
        },
      ],
    },
    'generic-http': {
      id: 'generic-http',
      title: 'Generic HTTP',
      description: 'Any HTTP client can integrate. No SDK required.',
      blocks: [
        { type: 'http', method: 'POST', path: '/api/v1/sms/send' },
        { type: 'p', text: 'Header: X-API-Key. Content-Type: application/json.' },
        {
          type: 'warn',
          text: 'Never call Nova SMS from browser JavaScript with a live key. Browser → your backend → Nova SMS.',
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
            ['Status', 'GET /sms/{uid}'],
            ['Default sender', 'TALKSASA_SENDER_ID (default TALK-SASA), configurable'],
            ['Retries', 'Up to novastack.sms.max-retries (default 3) on 408/502/503/504'],
            ['Batch chunk', 'SMS_BATCH_SIZE default 100'],
          ],
        },
        {
          type: 'p',
          text: 'The public API does not change if another provider is added. Super Admin can inspect GET /api/v1/admin/talksasa.',
        },
      ],
    },
  }
}
