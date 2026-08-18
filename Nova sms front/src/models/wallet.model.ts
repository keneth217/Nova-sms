export type WalletTransactionType = 'TOPUP' | 'SMS_DEBIT' | 'REFUND' | 'ADJUSTMENT'
export type TopupStatus = 'PENDING' | 'COMPLETED' | 'FAILED'

export interface WalletBalance {
  walletId: string
  organizationId: string
  balance: number
  currency: string
  smsCost: number
  availableSms?: number
}

export interface WalletTopupRequest {
  amount: number
  phoneNumber: string
}

export interface StkPushResponse {
  transactionId: string
  checkoutRequestId: string | null
  merchantRequestId: string | null
  customerMessage: string | null
  status: TopupStatus
  amount: number
  phoneNumber: string
  mpesaReceipt: string | null
  resultCode: string | null
  resultDesc: string | null
  callbackReceived: boolean
  walletCredited: boolean
  updatedAt: string
}

export interface WalletTransaction {
  id: string
  organizationId: string
  type: WalletTransactionType
  amount: number
  balanceBefore: number
  balanceAfter: number
  reference: string | null
  description: string | null
  mpesaReceipt: string | null
  phoneNumber: string | null
  checkoutRequestId: string | null
  status: TopupStatus | null
  resultCode: string | null
  resultDesc: string | null
  createdAt: string
}

export interface PaymentInstructions {
  paybill: string
  accountNumber: string
  businessName: string
  notes: string[]
}
