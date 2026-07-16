export type OrganizationStatus = 'ACTIVE' | 'SUSPENDED' | 'PENDING' | 'EXPIRED'
export type OrganizationAccountType = 'BUSINESS' | 'EVENT'

export interface Organization {
  id: string
  name: string
  email: string
  phone: string
  apiKey: string
  mpesaAccountRef: string
  status: OrganizationStatus
  accountType: OrganizationAccountType
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
