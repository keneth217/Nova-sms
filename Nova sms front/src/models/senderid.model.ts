export type SenderIdStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface SenderIdRequest {
  senderName: string
}

export interface SenderId {
  id: string
  senderName: string
  status: SenderIdStatus
  platformDefault: boolean
  reason: string | null
  createdAt: string
  updatedAt: string
}

export interface SenderIdReviewRequest {
  status: SenderIdStatus
  reason?: string
}
