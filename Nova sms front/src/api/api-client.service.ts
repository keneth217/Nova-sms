import api from './axios'
import type { ApiResponse, Page, PageRequest } from '@/models/auth.model'
import type {
  AdminCreateOrganizationRequest,
  ApiClient,
  ApiClientCreated,
  ApiClientUsage,
  CreateApiClientRequest,
  DeveloperConfig,
  UpdateApiClientRequest,
} from '@/models/api-client.model'
import type { AdminOrganization } from '@/models/organization.model'
import type { SmsMessage } from '@/models/sms.model'

class ApiClientService {
  async listMine(): Promise<ApiClient[]> {
    const { data } = await api.get<ApiResponse<ApiClient[]>>('/api-clients')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load API clients')
    return data.data
  }

  async createMine(payload: CreateApiClientRequest): Promise<ApiClientCreated> {
    const { data } = await api.post<ApiResponse<ApiClientCreated>>('/api-clients', payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to create API client')
    return data.data
  }

  async updateMine(clientId: string, payload: UpdateApiClientRequest): Promise<ApiClient> {
    const { data } = await api.patch<ApiResponse<ApiClient>>(`/api-clients/${clientId}`, payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to update API client')
    return data.data
  }

  async rotateMine(clientId: string): Promise<ApiClientCreated> {
    const { data } = await api.post<ApiResponse<ApiClientCreated>>(`/api-clients/${clientId}/rotate`)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to rotate API key')
    return data.data
  }

  async setEnabledMine(clientId: string, enabled: boolean): Promise<ApiClient> {
    const { data } = await api.patch<ApiResponse<ApiClient>>(`/api-clients/${clientId}/enabled`, null, {
      params: { enabled },
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to update API client')
    return data.data
  }

  async revokeMine(clientId: string): Promise<ApiClient> {
    const { data } = await api.post<ApiResponse<ApiClient>>(`/api-clients/${clientId}/revoke`)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to revoke API client')
    return data.data
  }

  async listAll(params: PageRequest & { organizationId?: string } = {}): Promise<Page<ApiClient>> {
    const { data } = await api.get<ApiResponse<Page<ApiClient>>>('/admin/api-clients', { params })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load API clients')
    return data.data
  }

  async createAdmin(payload: CreateApiClientRequest): Promise<ApiClientCreated> {
    const { data } = await api.post<ApiResponse<ApiClientCreated>>('/admin/api-clients', payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to create API client')
    return data.data
  }

  async updateAdmin(clientId: string, payload: UpdateApiClientRequest): Promise<ApiClient> {
    const { data } = await api.patch<ApiResponse<ApiClient>>(`/admin/api-clients/${clientId}`, payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to update API client')
    return data.data
  }

  async rotateAdmin(clientId: string): Promise<ApiClientCreated> {
    const { data } = await api.post<ApiResponse<ApiClientCreated>>(`/admin/api-clients/${clientId}/rotate`)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to rotate API key')
    return data.data
  }

  async setEnabledAdmin(clientId: string, enabled: boolean): Promise<ApiClient> {
    const { data } = await api.patch<ApiResponse<ApiClient>>(`/admin/api-clients/${clientId}/enabled`, null, {
      params: { enabled },
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to update API client')
    return data.data
  }

  async revokeAdmin(clientId: string): Promise<ApiClient> {
    const { data } = await api.post<ApiResponse<ApiClient>>(`/admin/api-clients/${clientId}/revoke`)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to revoke API client')
    return data.data
  }

  async createOrganization(payload: AdminCreateOrganizationRequest): Promise<AdminOrganization> {
    const { data } = await api.post<ApiResponse<AdminOrganization>>('/admin/organizations', payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to create organization')
    return data.data
  }

  async creditWallet(organizationId: string, amount: number, description?: string): Promise<AdminOrganization> {
    const { data } = await api.post<ApiResponse<AdminOrganization>>(
      `/admin/organizations/${organizationId}/wallet/credit`,
      { amount, description },
    )
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to credit wallet')
    return data.data
  }

  async developerConfig(): Promise<DeveloperConfig> {
    const { data } = await api.get<ApiResponse<DeveloperConfig>>('/admin/developer/config')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load API configuration')
    return data.data
  }

  async usage(clientId: string): Promise<ApiClientUsage> {
    const { data } = await api.get<ApiResponse<ApiClientUsage>>(`/admin/api-clients/${clientId}/usage`)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load API usage')
    return data.data
  }

  async testSend(
    clientId: string,
    payload: { recipient: string; message: string; senderId?: string },
  ): Promise<SmsMessage> {
    const { data } = await api.post<ApiResponse<SmsMessage>>(`/admin/api-clients/${clientId}/test-send`, payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to send test SMS')
    return data.data
  }

  async refreshStatus(messageId: string): Promise<SmsMessage> {
    const { data } = await api.get<ApiResponse<SmsMessage>>(`/admin/sms/${messageId}/status`)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to refresh SMS status')
    return data.data
  }
}

export const apiClientService = new ApiClientService()
