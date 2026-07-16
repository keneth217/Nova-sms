import api from './axios'
import type { ApiResponse } from '@/models/auth.model'
import type { SenderId, SenderIdRequest, SenderIdReviewRequest } from '@/models/senderid.model'
import { delay, isMockMode } from '@/utils/format'
import { mockSenderIds } from '@/mocks/data'

class SenderIdService {
  async list(): Promise<SenderId[]> {
    if (isMockMode()) {
      await delay(250)
      return [...mockSenderIds]
    }
    const { data } = await api.get<ApiResponse<SenderId[]>>('/sender-ids')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load sender IDs')
    return data.data
  }

  async request(payload: SenderIdRequest): Promise<SenderId> {
    if (isMockMode()) {
      await delay(400)
      const created: SenderId = {
        id: 'sid-' + Date.now(),
        senderName: payload.senderName.toUpperCase(),
        status: 'PENDING',
        platformDefault: false,
        reason: null,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
      mockSenderIds.unshift(created)
      return created
    }
    const { data } = await api.post<ApiResponse<SenderId>>('/sender-ids', payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to request sender ID')
    return data.data
  }

  async review(id: string, payload: SenderIdReviewRequest): Promise<SenderId> {
    if (isMockMode()) {
      await delay(300)
      const item = mockSenderIds.find((s) => s.id === id)
      if (!item) throw new Error('Sender ID not found')
      item.status = payload.status
      item.reason = payload.reason ?? null
      item.updatedAt = new Date().toISOString()
      return { ...item }
    }
    const { data } = await api.patch<ApiResponse<SenderId>>(`/sender-ids/${id}/review`, payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to review sender ID')
    return data.data
  }
}

export const senderIdService = new SenderIdService()
