export type WalletTransactionType = 'TOPUP' | 'SMS_DEBIT' | 'REFUND' | 'ADJUSTMENT'
export type TopupStatus = 'PENDING' | 'COMPLETED' | 'FAILED'
export type PaymentMethod = 'STK_PUSH' | 'PAYBILL'

export interface WalletBalance {
  walletId: string
  organizationId: string
  balance: number
  currency: string
  smsCost: number
  availableSms?: number
  paybill?: string
  accountNumber?: string
  businessName?: string
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
  callbackReceived?: boolean
  walletCredited?: boolean
  paymentMethod?: PaymentMethod | null
  paybill?: string | null
  accountNumber?: string | null
  organizationName?: string | null
  createdAt: string
}

export interface PaymentInstructions {
  paybill: string
  accountNumber: string
  businessName: string
  notes: string[]
}

export type MpesaReceiptSource = 'WALLET' | 'COLLECTION' | 'C2B_INBOUND' | 'NONE'

export interface MpesaReceiptLookup {
  mpesaReceipt: string
  found: boolean
  source: MpesaReceiptSource
  walletCredited: boolean
  needsManualRecovery?: boolean
  recoverableFromCallback?: boolean
  transactionId?: string | null
  organizationId?: string | null
  organizationName?: string | null
  amount?: number | null
  status?: TopupStatus | null
  billRef?: string | null
  message: string
}
