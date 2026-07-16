import api from './axios'
import type { ApiResponse, Page, PageRequest } from '@/models/auth.model'
import type {
  AdminOrganization,
  OrganizationStatus,
  PlatformOverview,
} from '@/models/organization.model'
import type { User } from '@/models/user.model'
import { delay, isMockMode } from '@/utils/format'
import { mockOrganizations, mockPlatformOverview, toPage } from '@/mocks/data'

class OrganizationService {
  async getOverview(): Promise<PlatformOverview> {
    if (isMockMode()) {
      await delay(300)
      return mockPlatformOverview
    }
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
    if (isMockMode()) {
      await delay(300)
      let items = [...mockOrganizations]
      if (params.status) items = items.filter((o) => o.status === params.status)
      if (params.search) {
        const q = params.search.toLowerCase()
        items = items.filter(
          (o) => o.name.toLowerCase().includes(q) || o.email.toLowerCase().includes(q),
        )
      }
      return toPage(items, params.page ?? 0, params.size ?? 20)
    }
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
    if (isMockMode()) {
      await delay(300)
      const org = mockOrganizations.find((o) => o.id === organizationId)
      if (!org) throw new Error('Organization not found')
      org.status = status
      return { ...org }
    }
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
    if (isMockMode()) {
      await delay(250)
      return toPage([], params.page ?? 0, params.size ?? 20)
    }
    const { data } = await api.get<ApiResponse<Page<User>>>('/admin/users', { params })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load users')
    return data.data
  }
}

export const organizationService = new OrganizationService()
