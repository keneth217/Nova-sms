import api from './axios'
import type { ApiResponse, Page, PageRequest } from '@/models/auth.model'
import type {
  AdminOrganization,
  Organization,
  OrganizationStatus,
  PlatformOverview,
} from '@/models/organization.model'
import type { User } from '@/models/user.model'
import type { TopupStatus, WalletTransaction } from '@/models/wallet.model'

class OrganizationService {
  async getCurrent(): Promise<Organization> {
    const { data } = await api.get<ApiResponse<Organization>>('/organizations/me')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load organization')
    return data.data
  }

  async getOverview(): Promise<PlatformOverview> {
    const { data } = await api.get<ApiResponse<Record<string, number>>>('/admin/overview')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load overview')
    const raw = data.data
    return {
      organizations: Number(raw.organizations ?? 0),
      users: Number(raw.users ?? 0),
      superAdmins: Number(raw.superAdmins ?? 0),
      totalSmsSent: Number(raw.totalSmsSent ?? 0),
      pendingSenderIds: Number(raw.pendingSenderIds ?? 0),
      pendingTopups: Number(raw.pendingTopups ?? 0),
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
}

export const organizationService = new OrganizationService()
