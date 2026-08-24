export type ApiClientStatus = 'ACTIVE' | 'DISABLED' | 'REVOKED'
export type ApiPermission =
  | 'SMS_SEND'
  | 'SMS_BULK'
  | 'SMS_STATUS'
  | 'SMS_HISTORY'
  | 'WALLET_READ'
  | 'WALLET_TOPUP'
  | 'MPESA_STK_PUSH'
  | 'MPESA_STATUS'
  | 'MPESA_C2B'

export const API_PERMISSIONS: { id: ApiPermission; label: string; hint: string }[] = [
  { id: 'SMS_SEND', label: 'SMS_SEND', hint: 'Send one SMS' },
  { id: 'SMS_BULK', label: 'SMS_BULK', hint: 'Bulk and scheduled SMS' },
  { id: 'SMS_STATUS', label: 'SMS_STATUS', hint: 'Look up SMS status' },
  { id: 'SMS_HISTORY', label: 'SMS_HISTORY', hint: 'List SMS history' },
  { id: 'WALLET_READ', label: 'WALLET_READ', hint: 'Show wallet balance and transactions on your site' },
  { id: 'WALLET_TOPUP', label: 'WALLET_TOPUP', hint: 'Let users top up via M-Pesa STK from your site' },
  { id: 'MPESA_STK_PUSH', label: 'MPESA_STK_PUSH', hint: 'Initiate STK or checkout at POST /api/v1/mpesa/stkpush or /checkout' },
  { id: 'MPESA_STATUS', label: 'MPESA_STATUS', hint: 'Read and refresh STK / checkout transaction status' },
  { id: 'MPESA_C2B', label: 'MPESA_C2B', hint: 'List C2B credits at GET /api/v1/mpesa/c2b/transactions (not Daraja registration)' },
]
export type OrganizationBillingModel = 'PREPAID' | 'MONTHLY' | 'INTERNAL'

export interface ApiClient {
  id: string
  organizationId: string
  organizationName?: string | null
  name: string
  clientCode: string
  apiKeyPrefix: string
  status: ApiClientStatus
  permissions: ApiPermission[]
  rateLimitPerMinute: number
  lastUsedAt?: string | null
  expiresAt?: string | null
  createdAt: string
}

export interface ApiClientCreated {
  client: ApiClient
  apiKey: string
}

export interface CreateApiClientRequest {
  organizationId?: string
  name: string
  clientCode?: string
  permissions?: ApiPermission[]
  rateLimitPerMinute?: number
}

export interface UpdateApiClientRequest {
  name?: string
  permissions?: ApiPermission[]
  rateLimitPerMinute?: number
}

export interface AdminCreateOrganizationRequest {
  name: string
  email: string
  phone: string
  accountType?: 'BUSINESS' | 'EVENT'
  billingModel?: OrganizationBillingModel
  adminFullName?: string
  adminPassword?: string
  smsCost?: number
  initialCredit?: number
}

export interface DeveloperConfig {
  publicBaseUrl: string
  apiBaseUrl: string
  openApiPath: string
  swaggerUiPath: string
}

export interface ApiClientUsageDailyPoint {
  date: string
  sent: number
  delivered: number
  failed: number
}

export interface ApiClientRequestDailyPoint {
  date: string
  requests: number
  success: number
  failed: number
  sms: number
  mpesa: number
  averageDurationMs: number
}

export interface ApiEndpointCount {
  path: string
  count: number
}

export interface ApiClientUsage {
  client: ApiClient
  totalSms: number
  successfulSms: number
  failedSms: number
  smsUnitsUsed: number
  walletBalance: number
  walletCurrency: string
  lastRequestAt?: string | null
  lastSmsAt?: string | null
  smsToday: number
  smsThisMonth: number
  daily: ApiClientUsageDailyPoint[]
  requestsToday: number
  requestsThisWeek: number
  requestsThisMonth: number
  successfulToday: number
  failedToday: number
  smsApiCallsToday: number
  mpesaApiCallsToday: number
  smsSendCallsThisMonth: number
  smsBulkCallsThisMonth: number
  mpesaStkCallsThisMonth: number
  mpesaStatusCallsThisMonth: number
  c2bVerifyCallsThisMonth: number
  mpesaStkInitiated: number
  mpesaStkSuccessful: number
  successRateThisMonth: number
  averageDurationMsThisMonth?: number | null
  http4xxThisMonth: number
  http5xxThisMonth: number
  requestDaily: ApiClientRequestDailyPoint[]
  topEndpoints: ApiEndpointCount[]
}

export interface ApiClientUsageCard {
  id: string
  name: string
  organizationName?: string | null
  status: string
  requestsToday: number
  successfulToday: number
  failedToday: number
  smsSent: number
  mpesaRequestsToday: number
  lastRequestAt?: string | null
}

export interface ApiClientUsageRank {
  id: string
  name: string
  organizationName?: string | null
  requests: number
}

export interface ApiClientUsageOverview {
  requestsToday: number
  requestsThisWeek: number
  requestsThisMonth: number
  clients: ApiClientUsageCard[]
  byClientThisMonth: ApiClientUsageRank[]
}

export interface ApiRequestLogRow {
  id: string
  requestId: string
  method: string
  path: string
  permission?: string | null
  resourceCategory: string
  status: number
  outcome: 'SUCCESS' | 'CLIENT_ERROR' | 'SERVER_ERROR'
  durationMs: number
  ipAddress?: string | null
  userAgent?: string | null
  createdAt: string
}
