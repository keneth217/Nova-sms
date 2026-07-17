import api from './axios'
import type { ApiResponse, Page, PageRequest } from '@/models/auth.model'
import type {
  BulkSmsRequest,
  BulkSmsResponse,
  ScheduleSmsRequest,
  SendSmsRequest,
  SmsMessage,
} from '@/models/sms.model'
import { delay, isMockMode, normalizePhone } from '@/utils/format'
import { mockSmsHistory, toPage } from '@/mocks/data'

class SmsService {
  async send(payload: SendSmsRequest): Promise<SmsMessage> {
    const recipient = normalizePhone(payload.recipient)
    if (isMockMode()) {
      await delay(500)
      return {
        id: 'sms-' + Date.now(),
        recipient,
        content: payload.message,
        senderId: payload.senderId || 'NOVASMS',
        status: 'QUEUED',
        cost: 1,
        batchId: null,
        scheduledAt: null,
        createdAt: new Date().toISOString(),
        sentAt: null,
        deliveredAt: null,
        failureReason: null,
      }
    }
    const { data } = await api.post<ApiResponse<SmsMessage>>('/sms/send', {
      ...payload,
      recipient,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to send SMS')
    return data.data
  }

  async sendBulk(payload: BulkSmsRequest): Promise<BulkSmsResponse> {
    const recipients = payload.recipients?.map((r) => normalizePhone(r))
    if (isMockMode()) {
      await delay(700)
      const list = recipients ?? []
      return {
        batchId: 'BATCH-' + Date.now(),
        queuedCount: list.length || 1,
        messages: list.slice(0, 5).map((recipient, i) => ({
          id: `sms-b-${Date.now()}-${i}`,
          recipient,
          content: payload.message,
          senderId: payload.senderId || 'NOVASMS',
          status: 'QUEUED' as const,
          cost: 1,
          batchId: 'BATCH-' + Date.now(),
          scheduledAt: null,
          createdAt: new Date().toISOString(),
          sentAt: null,
          deliveredAt: null,
          failureReason: null,
        })),
      }
    }
    const { data } = await api.post<ApiResponse<BulkSmsResponse>>('/sms/bulk', {
      ...payload,
      recipients,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to send bulk SMS')
    return data.data
  }

  async schedule(payload: ScheduleSmsRequest): Promise<BulkSmsResponse> {
    const recipients = payload.recipients?.map((r) => normalizePhone(r))
    const body: ScheduleSmsRequest = {
      ...payload,
      recipients,
      scheduledAt: new Date(payload.scheduledAt).toISOString(),
    }
    if (isMockMode()) {
      await delay(500)
      const list = recipients ?? []
      return {
        batchId: 'SCHED-' + Date.now(),
        queuedCount: list.length || (payload.groupId ? 1 : 0),
        messages: list.slice(0, 5).map((recipient, i) => ({
          id: `sms-s-${Date.now()}-${i}`,
          recipient,
          content: payload.message,
          senderId: payload.senderId || 'NOVASMS',
          status: 'SCHEDULED' as const,
          cost: 1,
          batchId: 'SCHED-' + Date.now(),
          scheduledAt: body.scheduledAt,
          createdAt: new Date().toISOString(),
          sentAt: null,
          deliveredAt: null,
          failureReason: null,
        })),
      }
    }
    const { data } = await api.post<ApiResponse<BulkSmsResponse>>('/sms/schedule', body)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to schedule SMS')
    return data.data
  }

  async getHistory(params: PageRequest = {}): Promise<Page<SmsMessage>> {
    if (isMockMode()) {
      await delay(300)
      return toPage(mockSmsHistory, params.page ?? 0, params.size ?? 20)
    }
    const { useAuthStore } = await import('@/stores/auth.store')
    const path = useAuthStore().isSuperAdmin ? '/admin/sms' : '/sms/history'
    const { data } = await api.get<ApiResponse<Page<SmsMessage>>>(path, { params })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load SMS history')
    return data.data
  }
}

export const smsService = new SmsService()
