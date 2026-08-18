import api from './axios'
import type { ApiResponse, Page, PageRequest } from '@/models/auth.model'
import type {
  BulkSmsRequest,
  BulkSmsResponse,
  MessageChannel,
  ScheduleSmsRequest,
  SendSmsRequest,
  SmsMessage,
} from '@/models/sms.model'
import { normalizePhone } from '@/utils/format'

function channelRoot(channel?: MessageChannel) {
  return channel === 'WHATSAPP' ? '/whatsapp' : '/sms'
}

class SmsService {
  async send(payload: SendSmsRequest, channel: MessageChannel = 'SMS'): Promise<SmsMessage> {
    const recipient = normalizePhone(payload.recipient)
    const { data } = await api.post<ApiResponse<SmsMessage>>(`${channelRoot(channel)}/send`, {
      ...payload,
      recipient,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to send message')
    return data.data
  }

  async sendBulk(payload: BulkSmsRequest, channel: MessageChannel = 'SMS'): Promise<BulkSmsResponse> {
    const recipients = payload.recipients?.map((r) => normalizePhone(r))
    const { data } = await api.post<ApiResponse<BulkSmsResponse>>(`${channelRoot(channel)}/bulk`, {
      ...payload,
      recipients,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to send bulk messages')
    return data.data
  }

  async schedule(payload: ScheduleSmsRequest, channel: MessageChannel = 'SMS'): Promise<BulkSmsResponse> {
    const recipients = payload.recipients?.map((r) => normalizePhone(r))
    const body: ScheduleSmsRequest = {
      ...payload,
      recipients,
      scheduledAt: new Date(payload.scheduledAt).toISOString(),
    }
    const { data } = await api.post<ApiResponse<BulkSmsResponse>>(`${channelRoot(channel)}/schedule`, body)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to schedule messages')
    return data.data
  }

  async getHistory(
    params: PageRequest = {},
    channel: MessageChannel = 'SMS',
  ): Promise<Page<SmsMessage>> {
    const { useAuthStore } = await import('@/stores/auth.store')
    const auth = useAuthStore()
    const path = auth.isSuperAdmin
      ? '/admin/sms'
      : `${channelRoot(channel)}/history`
    const { data } = await api.get<ApiResponse<Page<SmsMessage>>>(path, { params })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load history')
    return data.data
  }

  async getById(id: string): Promise<SmsMessage> {
    const { data } = await api.get<ApiResponse<SmsMessage>>(`/sms/${id}`)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load SMS')
    return data.data
  }

  async refreshStatus(id: string): Promise<SmsMessage> {
    const { data } = await api.get<ApiResponse<SmsMessage>>(`/sms/${id}/status`)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to refresh SMS status')
    return data.data
  }
}

export const smsService = new SmsService()
