export type ApiClientStatus = 'ACTIVE' | 'DISABLED' | 'REVOKED'
export type ApiPermission =
  | 'SMS_SEND'
  | 'SMS_BULK'
  | 'SMS_STATUS'
  | 'SMS_HISTORY'
  | 'WALLET_READ'
  | 'WALLET_TOPUP'

export const API_PERMISSIONS: { id: ApiPermission; label: string; hint: string }[] = [
  { id: 'SMS_SEND', label: 'SMS_SEND', hint: 'Send one SMS' },
  { id: 'SMS_BULK', label: 'SMS_BULK', hint: 'Bulk and scheduled SMS' },
  { id: 'SMS_STATUS', label: 'SMS_STATUS', hint: 'Look up SMS status' },
  { id: 'SMS_HISTORY', label: 'SMS_HISTORY', hint: 'List SMS history' },
  { id: 'WALLET_READ', label: 'WALLET_READ', hint: 'Show wallet balance and transactions on your site' },
  { id: 'WALLET_TOPUP', label: 'WALLET_TOPUP', hint: 'Let users top up via M-Pesa STK from your site' },
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
}
