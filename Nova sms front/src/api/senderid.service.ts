import api from './axios'
import type { ApiResponse } from '@/models/auth.model'
import type { SenderId, SenderIdRequest, SenderIdReviewRequest } from '@/models/senderid.model'

class SenderIdService {
  async list(): Promise<SenderId[]> {
    const { useAuthStore } = await import('@/stores/auth.store')
    const path = useAuthStore().isSuperAdmin ? '/admin/sender-ids' : '/sender-ids'
    const { data } = await api.get<ApiResponse<SenderId[]>>(path)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load sender IDs')
    return data.data
  }

  async request(payload: SenderIdRequest): Promise<SenderId> {
    const { data } = await api.post<ApiResponse<SenderId>>('/sender-ids', payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to request sender ID')
    return data.data
  }

  async review(id: string, payload: SenderIdReviewRequest): Promise<SenderId> {
    const { data } = await api.patch<ApiResponse<SenderId>>(`/sender-ids/${id}/review`, payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to review sender ID')
    return data.data
  }
}

export const senderIdService = new SenderIdService()
