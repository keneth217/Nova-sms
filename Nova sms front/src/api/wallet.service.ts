import api from './axios'
import type { ApiResponse, Page, PageRequest } from '@/models/auth.model'
import type {
  MpesaReceiptLookup,
  StkPushResponse,
  TopupStatus,
  WalletBalance,
  WalletTopupRequest,
  WalletTransaction,
  WalletTransactionType,
} from '@/models/wallet.model'
import { normalizePhone } from '@/utils/format'

class WalletService {
  async getBalance(): Promise<WalletBalance> {
    const { data } = await api.get<ApiResponse<WalletBalance>>('/wallet/balance')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load balance')
    const raw = data.data as WalletBalance & {
      shortcode?: string
      mpesaAccountRef?: string
      account_number?: string
    }
    return {
      ...raw,
      paybill: raw.paybill || raw.shortcode,
      accountNumber: raw.accountNumber || raw.mpesaAccountRef || raw.account_number,
    }
  }

  async topUp(payload: WalletTopupRequest): Promise<StkPushResponse> {
    const phoneNumber = normalizePhone(payload.phoneNumber)
    const { data } = await api.post<ApiResponse<StkPushResponse>>('/wallet/topup', {
      ...payload,
      phoneNumber,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Top-up failed')
    return data.data
  }

  async getTopupStatus(transactionId: string): Promise<StkPushResponse> {
    const { data } = await api.get<ApiResponse<StkPushResponse>>(`/wallet/topup/${transactionId}`)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to check payment status')
    return data.data
  }

  async checkTopup(transactionId: string): Promise<StkPushResponse> {
    const { data } = await api.post<ApiResponse<StkPushResponse>>(
      `/wallet/topup/${transactionId}/check`,
    )
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to query payment status')
    return data.data
  }

  async verifyReceipt(mpesaReceipt: string): Promise<MpesaReceiptLookup> {
    const { data } = await api.post<ApiResponse<MpesaReceiptLookup>>('/wallet/topup/verify-receipt', {
      mpesaReceipt: mpesaReceipt.trim(),
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to verify receipt')
    return data.data
  }

  async getTransactions(
    params: PageRequest & {
      organizationId?: string
      type?: WalletTransactionType
      status?: TopupStatus[]
    } = {},
  ): Promise<Page<WalletTransaction>> {
    const { data } = await api.get<ApiResponse<Page<WalletTransaction>>>('/wallet/transactions', {
      params,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load transactions')
    return data.data
  }
}

export const walletService = new WalletService()
