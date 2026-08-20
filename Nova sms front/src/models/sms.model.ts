export type MessageChannel = 'SMS' | 'WHATSAPP'

export type MessageStatus =
  | 'PENDING'
  | 'QUEUED'
  | 'PROCESSING'
  | 'ACCEPTED'
  | 'SENT'
  | 'SCHEDULED'
  | 'DELIVERED'
  | 'FAILED'
  | 'REJECTED'
  | 'CANCELLED'

export function isBillableFailure(status?: MessageStatus | string | null): boolean {
  return status === 'FAILED' || status === 'REJECTED' || status === 'CANCELLED'
}

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
  messageId?: string | null
  organizationId?: string | null
  organizationName?: string | null
  apiClientId?: string | null
  recipient: string
  content: string
  channel?: MessageChannel | null
  senderId: string
  status: MessageStatus
  cost: number
  smsUnits?: number | null
  encoding?: string | null
  characterCount?: number | null
  unitPrice?: number | null
  currency?: string | null
  provider?: string | null
  providerMessageId?: string | null
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
  recipientCount?: number
  smsUnits?: number
  status?: string
  sourceBatchId?: string | null
  resentCount?: number | null
  skippedCount?: number | null
  failedCount?: number | null
  messages: SmsMessage[]
}

export interface TalkSasaSmsItem {
  uid?: string | null
  recipient?: string | null
  senderId?: string | null
  message?: string | null
  status?: string | null
  type?: string | null
  direction?: string | null
  cost?: string | null
  smsCount?: number | null
  createdAt?: string | null
  novaMessage?: SmsMessage | null
}

export interface TalkSasaSmsList {
  configured: boolean
  reachable: boolean
  errorMessage?: string | null
  page?: number | null
  perPage?: number | null
  total?: number | null
  lastPage?: number | null
  items: TalkSasaSmsItem[]
}

export interface TalkSasaSmsView {
  configured: boolean
  reachable: boolean
  errorMessage?: string | null
  item?: TalkSasaSmsItem | null
}

export interface SmsHistoryFilters {
  status?: MessageStatus | ''
  senderId?: string
  dateFrom?: string
  dateTo?: string
  search?: string
}
