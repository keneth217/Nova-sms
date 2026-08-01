export type MessageStatus = 'PENDING' | 'SCHEDULED' | 'DELIVERED' | 'FAILED'

export interface SendSmsRequest {
  recipient: string
  message: string
  senderId?: string
}

export interface BulkSmsRequest {
  recipients?: string[]
  message: string
  senderId?: string
  groupId?: string
}

export interface ScheduleSmsRequest {
  recipients?: string[]
  message: string
  senderId?: string
  scheduledAt: string
  groupId?: string
}

export interface SmsMessage {
  id: string
  organizationId?: string | null
  organizationName?: string | null
  recipient: string
  content: string
  senderId: string
  status: MessageStatus
  cost: number
  batchId: string | null
  scheduledAt: string | null
  createdAt: string
  sentAt: string | null
  deliveredAt: string | null
  failureReason: string | null
}

export interface BulkSmsResponse {
  batchId: string
  queuedCount: number
  messages: SmsMessage[]
}

export interface SmsHistoryFilters {
  status?: MessageStatus | ''
  senderId?: string
  dateFrom?: string
  dateTo?: string
  search?: string
}
