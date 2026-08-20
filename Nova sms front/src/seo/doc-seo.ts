import { PUBLIC_DOC_SLUGS, type PublicDocSlug } from '@/seo/public-paths'

export const publicDocSeo: Record<
  PublicDocSlug,
  { title: string; description: string }
> = {
  'quick-start': {
    title: 'Nova SMS API quick start — send your first SMS',
    description:
      'Create an API key, POST /api/v1/sms/send from your backend, and read message status from Nova SMS.',
  },
  authentication: {
    title: 'Nova SMS API authentication',
    description:
      'Authenticate with X-API-Key. API clients belong to organizations. Keys are hashed and shown only once.',
  },
  'send-sms': {
    title: 'Send SMS API — Nova SMS',
    description:
      'POST /api/v1/sms/send with recipient and message. Optional sender ID. Permission SMS_SEND.',
  },
  'bulk-sms': {
    title: 'Bulk SMS API — Nova SMS',
    description: 'Send many SMS messages through Nova SMS with SMS_BULK permission and wallet debit.',
  },
  'retry-failed': {
    title: 'Retry failed SMS — Nova SMS API',
    description:
      'Resend FAILED SMS without duplicating SENT recipients. GET a batch, then POST resend-failed or POST /sms/{id}/resend.',
  },
  status: {
    title: 'SMS status API — Nova SMS',
    description: 'Read delivery status for a message id with GET /api/v1/sms/{id}/status.',
  },
  history: {
    title: 'SMS history API — Nova SMS',
    description: 'Page organization SMS history with SMS_HISTORY permission.',
  },
  wallet: {
    title: 'M-Pesa STK Push API example — Nova SMS wallet',
    description:
      'Show SMS balance and start M-Pesa STK top-ups with WALLET_READ and WALLET_TOPUP. Poll until walletCredited is true.',
  },
  errors: {
    title: 'Nova SMS API errors',
    description: 'HTTP statuses and JSON message field used when a Nova SMS API call fails.',
  },
  idempotency: {
    title: 'Nova SMS API idempotency',
    description: 'Retry SMS send requests safely with idempotency keys so duplicates are not billed twice.',
  },
  'rate-limits': {
    title: 'Nova SMS API rate limits',
    description: 'Per-client rate limits for the Nova SMS REST API.',
  },
  'spring-boot': {
    title: 'Nova SMS Spring Boot integration',
    description: 'Call the Nova SMS API from a Spring Boot backend.',
  },
  nodejs: {
    title: 'Nova SMS Node.js integration',
    description: 'Send SMS from Node.js using the Nova SMS REST API.',
  },
  php: {
    title: 'Nova SMS PHP integration',
    description: 'Send SMS from PHP using the Nova SMS REST API.',
  },
  python: {
    title: 'Nova SMS Python integration',
    description: 'Send SMS from Python using the Nova SMS REST API.',
  },
  'generic-http': {
    title: 'Nova SMS HTTP integration',
    description: 'Generic HTTP examples for the Nova SMS API.',
  },
}

export const publicDocRoutes = PUBLIC_DOC_SLUGS.map((slug) => ({
  path: `developers/${slug}`,
  name: `docs-${slug}`,
  slug,
  seoTitle: publicDocSeo[slug].title,
  description: publicDocSeo[slug].description,
}))
