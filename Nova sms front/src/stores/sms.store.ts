import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  BulkSmsRequest,
  BulkSmsResponse,
  ScheduleSmsRequest,
  SendSmsRequest,
  SmsHistoryFilters,
  SmsMessage,
} from '@/models/sms.model'
import type { SenderId } from '@/models/senderid.model'
import { smsService } from '@/api/sms.service'
import { senderIdService } from '@/api/senderid.service'

export const useSmsStore = defineStore('sms', () => {
  const history = ref<SmsMessage[]>([])
  const senderIds = ref<SenderId[]>([])
  const lastSend = ref<SmsMessage | null>(null)
  const lastBulk = ref<BulkSmsResponse | null>(null)
  const filters = ref<SmsHistoryFilters>({
    status: '',
    senderId: '',
    dateFrom: '',
    dateTo: '',
    search: '',
  })
  const loading = ref(false)
  const error = ref<string | null>(null)
  const totalElements = ref(0)

  async function fetchHistory(page = 0, size = 20) {
    loading.value = true
    error.value = null
    try {
      const result = await smsService.getHistory({ page, size })
      let items = result.content
      if (filters.value.status) {
        items = items.filter((m) => m.status === filters.value.status)
      }
      if (filters.value.senderId) {
        items = items.filter((m) => m.senderId === filters.value.senderId)
      }
      if (filters.value.search) {
        const q = filters.value.search.toLowerCase()
        items = items.filter(
          (m) =>
            m.recipient.includes(q) ||
            m.content.toLowerCase().includes(q) ||
            m.senderId.toLowerCase().includes(q) ||
            (m.organizationName || '').toLowerCase().includes(q),
        )
      }
      history.value = items
      totalElements.value = result.totalElements
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load history'
    } finally {
      loading.value = false
    }
  }

  async function fetchSenderIds() {
    try {
      senderIds.value = await senderIdService.list()
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load sender IDs'
    }
  }

  async function sendSms(payload: SendSmsRequest) {
    loading.value = true
    error.value = null
    try {
      lastSend.value = await smsService.send(payload)
      return lastSend.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Send failed'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function sendBulk(payload: BulkSmsRequest) {
    loading.value = true
    error.value = null
    try {
      lastBulk.value = await smsService.sendBulk(payload)
      return lastBulk.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Bulk send failed'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function scheduleSms(payload: ScheduleSmsRequest) {
    loading.value = true
    error.value = null
    try {
      lastBulk.value = await smsService.schedule(payload)
      return lastBulk.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Schedule failed'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function requestSenderId(senderName: string) {
    loading.value = true
    error.value = null
    try {
      const created = await senderIdService.request({ senderName })
      senderIds.value = [created, ...senderIds.value]
      return created
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Request failed'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    history,
    senderIds,
    lastSend,
    lastBulk,
    filters,
    loading,
    error,
    totalElements,
    fetchHistory,
    fetchSenderIds,
    sendSms,
    sendBulk,
    scheduleSms,
    requestSenderId,
  }
})
