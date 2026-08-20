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
  notificationsEnabled?: boolean | null
  lowBalanceThreshold?: number | null
  platformNotificationsEnabled?: boolean | null
  platformLowBalanceThreshold?: number | null
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
  totalOrgWalletBalance?: number
  currency?: string
  lowBalanceThreshold?: number | null
}

export interface PlatformNotificationSettings {
  enabled: boolean
  lowBalanceThreshold: number
  portalUrl?: string | null
  welcomeTemplate?: string | null
  topupTemplate?: string | null
  collectionTemplate?: string | null
  lowBalanceTemplate?: string | null
  platformTopupTemplate?: string | null
  providerLowTemplate?: string | null
  providerExposureTemplate?: string | null
  talksasaLastRemaining?: number | null
  talksasaLowAlerted?: boolean | null
  talksasaExposureAlerted?: boolean | null
  collectionAccounts?: string[] | null
  collectionNotifyPhones?: string[] | null
}

export interface C2bCallbackUrls {
  shortcode?: string
  responseType?: string
  confirmationUrl: string
  validationUrl: string
  success?: string
  alreadyRegistered?: string
  errorCode?: string
  message?: string
  darajaResponse?: string
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
