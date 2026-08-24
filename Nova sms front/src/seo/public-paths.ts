/** Canonical origin used in sitemap, robots, and absolute social URLs. */
export const SITE_ORIGIN = 'https://novasms.novastack.co.ke'

export const SITE_NAME = 'Nova SMS'

export const DEFAULT_DESCRIPTION =
  'Send bulk SMS in Kenya with Nova SMS. Prepaid wallet, M-Pesa STK Push and Paybill top-up, delivery reports, and a REST API for developers.'

export const CONTACT_EMAIL = 'support@novastack.com'

export const PLATFORM_PAYBILL = '5687394'

/** Public documentation paths under /developers (indexable). */
export const PUBLIC_DOC_SLUGS = [
  'quick-start',
  'authentication',
  'send-sms',
  'bulk-sms',
  'retry-failed',
  'status',
  'history',
  'wallet',
  'mpesa',
  'mpesa-stk',
  'mpesa-checkout',
  'mpesa-status',
  'mpesa-c2b',
  'mpesa-callbacks',
  'errors',
  'idempotency',
  'rate-limits',
  'spring-boot',
  'nodejs',
  'php',
  'python',
  'generic-http',
] as const

export type PublicDocSlug = (typeof PUBLIC_DOC_SLUGS)[number]

/** Canonical public URLs included in sitemap.xml. Auth and dashboard paths are omitted. */
export const PUBLIC_SITEMAP_PATHS: string[] = [
  '/',
  '/sms-gateway',
  '/mpesa-stk-push',
  '/mpesa-paybill',
  '/webhooks',
  '/sms-api',
  '/pricing',
  '/about',
  '/contact',
  '/faq',
  '/developers',
  ...PUBLIC_DOC_SLUGS.map((slug) => `/developers/${slug}`),
  '/data-bundles',
  '/terms',
  '/privacy',
  '/acceptable-use',
]
