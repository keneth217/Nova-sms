export type OrganizationStatus = 'ACTIVE' | 'SUSPENDED' | 'PENDING' | 'EXPIRED'
export type OrganizationAccountType = 'BUSINESS' | 'EVENT'
export type OrganizationBillingModel = 'PREPAID' | 'MONTHLY' | 'INTERNAL'

export interface Organization {
  id: string
  name: string
  email: string
  phone: string
  apiKey?: string
  mpesaAccountRef: string
  status: OrganizationStatus
  accountType: OrganizationAccountType
  billingModel?: OrganizationBillingModel
  expiresAt: string | null
  activeDays?: number | null
  createdAt: string
  walletId?: string | null
  walletBalance?: number | null
  walletCurrency?: string | null
}

export interface AdminOrganization extends Organization {
  smsCost: number
  walletBalance: number
  currency: string
  userCount: number
}

export interface PlatformOverview {
  organizations: number
  users: number
  superAdmins: number
  totalSmsSent?: number
  revenue?: number
  pendingSenderIds?: number
  pendingTopups?: number
}

export interface PlatformBilling {
  provider: string
  defaultSenderId: string
  customerSmsPrice: number
  providerCost: number
  grossMargin: number
  currency: string
  totalSmsSent: number
  totalSmsUnits: number
  totalCustomerRevenue: number
  totalEstimatedProviderCost: number
  totalGrossMargin: number
}

export interface TalkSasaAccount {
  configured: boolean
  reachable: boolean
  errorMessage?: string | null
  profile?: {
    name?: string | null
    email?: string | null
    phone?: string | null
    country?: string | null
    timezone?: string | null
    status?: string | null
  } | null
  balance?: {
    remainingUnits?: number | null
    totalUnits?: number | null
    usedUnits?: number | null
    unitType?: string | null
    expiredOn?: string | null
  } | null
}
