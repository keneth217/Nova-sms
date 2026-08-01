import api from './axios'
import type { ApiResponse, Page, PageRequest } from '@/models/auth.model'
import type {
  PaymentInstructions,
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
    return data.data
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

  getPaymentInstructions(accountRef = 'YOUR-ACCOUNT'): PaymentInstructions {
    return {
      paybill: '522522',
      accountNumber: accountRef,
      businessName: 'Nova SMS Gateway',
      notes: [
        'Use your organization account reference as the M-Pesa account number.',
        'Top-ups usually reflect within 1–2 minutes after confirmation.',
      ],
    }
  }
}

export const walletService = new WalletService()
