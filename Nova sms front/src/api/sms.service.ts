import api from './axios'
import type { ApiResponse, Page, PageRequest } from '@/models/auth.model'
import type {
  BulkSmsRequest,
  BulkSmsResponse,
  ScheduleSmsRequest,
  SendSmsRequest,
  SmsMessage,
} from '@/models/sms.model'
import { normalizePhone } from '@/utils/format'

class SmsService {
  async send(payload: SendSmsRequest): Promise<SmsMessage> {
    const recipient = normalizePhone(payload.recipient)
    const { data } = await api.post<ApiResponse<SmsMessage>>('/sms/send', {
      ...payload,
      recipient,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to send SMS')
    return data.data
  }

  async sendBulk(payload: BulkSmsRequest): Promise<BulkSmsResponse> {
    const recipients = payload.recipients?.map((r) => normalizePhone(r))
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
    const { data } = await api.post<ApiResponse<BulkSmsResponse>>('/sms/schedule', body)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to schedule SMS')
    return data.data
  }

  async getHistory(params: PageRequest = {}): Promise<Page<SmsMessage>> {
    const { useAuthStore } = await import('@/stores/auth.store')
    const path = useAuthStore().isSuperAdmin ? '/admin/sms' : '/sms/history'
    const { data } = await api.get<ApiResponse<Page<SmsMessage>>>(path, { params })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load SMS history')
    return data.data
  }
}

export const smsService = new SmsService()
