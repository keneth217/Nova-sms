import api from './axios'
import type { ApiResponse, Page, PageRequest } from '@/models/auth.model'
import type {
  AdminOrganization,
  Organization,
  OrganizationStatus,
  PlatformOverview,
  TalkSasaAccount,
  PlatformBilling,
  PlatformNotificationSettings,
  C2bCallbackUrls,
} from '@/models/organization.model'
import type { User } from '@/models/user.model'
import type { TopupStatus, StkPushResponse, WalletTransaction, MpesaReceiptLookup } from '@/models/wallet.model'
import type { PaybillCollectionDashboard } from '@/models/collection.model'

class OrganizationService {
  async getCurrent(): Promise<Organization> {
    const { data } = await api.get<ApiResponse<Organization>>('/organizations/me')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load organization')
    return data.data
  }

  async updateSettings(payload: {
    name?: string
    email?: string
    phone?: string
    notificationsEnabled: boolean
    lowBalanceThreshold: number
  }): Promise<Organization> {
    const { data } = await api.patch<ApiResponse<Organization>>('/organizations/me/settings', payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to save settings')
    return data.data
  }

  async getNotificationSettings(): Promise<PlatformNotificationSettings> {
    const { data } = await api.get<ApiResponse<PlatformNotificationSettings>>('/admin/notifications')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load notification settings')
    return data.data
  }

  async updateNotificationSettings(payload: {
    enabled?: boolean
    lowBalanceThreshold?: number
    portalUrl?: string
    welcomeTemplate?: string
    topupTemplate?: string
    collectionTemplate?: string
    lowBalanceTemplate?: string
    platformTopupTemplate?: string
    providerLowTemplate?: string
    providerExposureTemplate?: string
    collectionAccounts?: string[]
    collectionNotifyPhones?: string[]
  }): Promise<PlatformNotificationSettings> {
    const { data } = await api.put<ApiResponse<PlatformNotificationSettings>>('/admin/notifications', payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to save SMS settings')
    return data.data
  }

  async getC2bUrls(): Promise<C2bCallbackUrls> {
    const { data } = await api.get<ApiResponse<C2bCallbackUrls>>('/admin/mpesa/c2b/urls')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load C2B URLs')
    return data.data
  }

  async registerC2bUrls(): Promise<C2bCallbackUrls> {
    const { data } = await api.post<ApiResponse<C2bCallbackUrls>>('/admin/mpesa/c2b/register')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to register C2B URLs')
    return data.data
  }

  async getTalkSasaAccount(): Promise<TalkSasaAccount> {
    const { data } = await api.get<ApiResponse<TalkSasaAccount>>('/admin/talksasa')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load TalkSasa account')
    return data.data
  }

  async getBilling(): Promise<PlatformBilling> {
    const { data } = await api.get<ApiResponse<PlatformBilling>>('/admin/billing')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load billing')
    return data.data
  }

  async updateBilling(payload: {
    customerSmsPrice?: number
    providerCost?: number
    currency?: string
  }): Promise<PlatformBilling> {
    const { data } = await api.put<ApiResponse<PlatformBilling>>('/admin/billing', payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to update billing')
    return data.data
  }

  async getOverview(): Promise<PlatformOverview> {
    const { data } = await api.get<ApiResponse<PlatformOverview>>('/admin/overview')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load overview')
    const raw = data.data
    return {
      organizations: Number(raw.organizations ?? 0),
      users: Number(raw.users ?? 0),
      superAdmins: Number(raw.superAdmins ?? 0),
      totalSmsSent: Number(raw.totalSmsSent ?? 0),
      pendingSenderIds: Number(raw.pendingSenderIds ?? 0),
      pendingTopups: Number(raw.pendingTopups ?? 0),
      totalOrgWalletBalance: Number(raw.totalOrgWalletBalance ?? 0),
      currency: raw.currency || 'KES',
      lowBalanceThreshold:
        raw.lowBalanceThreshold == null ? null : Number(raw.lowBalanceThreshold),
    }
  }

  async listOrganizations(
    params: PageRequest & {
      status?: OrganizationStatus
      search?: string
    } = {},
  ): Promise<Page<AdminOrganization>> {
    const { data } = await api.get<ApiResponse<Page<AdminOrganization>>>('/admin/organizations', {
      params,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load organizations')
    return data.data
  }

  async updateOrganizationStatus(
    organizationId: string,
    status: OrganizationStatus,
  ): Promise<AdminOrganization> {
    const { data } = await api.patch<ApiResponse<AdminOrganization>>(
      `/admin/organizations/${organizationId}/status`,
      null,
      { params: { status } },
    )
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to update status')
    return data.data
  }

  async listUsers(
    params: PageRequest & {
      role?: string
      organizationId?: string
      search?: string
    } = {},
  ): Promise<Page<User>> {
    const { data } = await api.get<ApiResponse<Page<User>>>('/admin/users', { params })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load users')
    return data.data
  }

  async listTopups(
    params: PageRequest & { status?: TopupStatus } = {},
  ): Promise<Page<WalletTransaction>> {
    const { data } = await api.get<ApiResponse<Page<WalletTransaction>>>('/admin/topups', {
      params,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load top-ups')
    return data.data
  }

  async getCollections(
    params: PageRequest & { billRef?: string } = {},
  ): Promise<PaybillCollectionDashboard> {
    const { data } = await api.get<ApiResponse<PaybillCollectionDashboard>>('/admin/collections', {
      params,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load collections')
    return data.data
  }

  async checkTopup(transactionId: string): Promise<StkPushResponse> {
    const { data } = await api.post<ApiResponse<StkPushResponse>>(`/admin/topups/${transactionId}/check`)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to query Safaricom')
    return data.data
  }

  async verifyTopupReceipt(mpesaReceipt: string): Promise<MpesaReceiptLookup> {
    const { data } = await api.post<ApiResponse<MpesaReceiptLookup>>('/admin/topups/verify-receipt', {
      mpesaReceipt: mpesaReceipt.trim(),
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to verify receipt')
    return data.data
  }

  async creditTopupReceipt(payload: {
    mpesaReceipt: string
    accountNumber?: string
    amount?: number
  }): Promise<MpesaReceiptLookup> {
    const { data } = await api.post<ApiResponse<MpesaReceiptLookup>>('/admin/topups/credit-receipt', {
      mpesaReceipt: payload.mpesaReceipt.trim(),
      accountNumber: payload.accountNumber?.trim() || undefined,
      amount: payload.amount,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to credit receipt')
    return data.data
  }
}

export const organizationService = new OrganizationService()
