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
import { delay, isMockMode, normalizePhone } from '@/utils/format'
import { mockPaymentInstructions, mockTransactions, mockWalletBalance, toPage } from '@/mocks/data'

class WalletService {
  async getBalance(): Promise<WalletBalance> {
    if (isMockMode()) {
      await delay(250)
      return { ...mockWalletBalance }
    }
    const { data } = await api.get<ApiResponse<WalletBalance>>('/wallet/balance')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load balance')
    return data.data
  }

  async topUp(payload: WalletTopupRequest): Promise<StkPushResponse> {
    const phoneNumber = normalizePhone(payload.phoneNumber)
    if (isMockMode()) {
      await delay(600)
      return {
        transactionId: 'tx-mock-' + Date.now(),
        checkoutRequestId: 'ws_CO_mock',
        merchantRequestId: 'mr_mock',
        customerMessage: 'STK push sent. Enter your M-Pesa PIN to complete.',
        status: 'PENDING',
        amount: payload.amount,
        phoneNumber,
        mpesaReceipt: null,
        resultCode: null,
        resultDesc: null,
        callbackReceived: false,
        walletCredited: false,
        updatedAt: new Date().toISOString(),
      }
    }
    const { data } = await api.post<ApiResponse<StkPushResponse>>('/wallet/topup', {
      ...payload,
      phoneNumber,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Top-up failed')
    return data.data
  }

  async getTopupStatus(transactionId: string): Promise<StkPushResponse> {
    if (isMockMode()) {
      await delay(400)
      const startedAt = Number(transactionId.split('tx-mock-')[1]) || Date.now()
      const done = Date.now() - startedAt > 12_000
      const last = mockTransactions[0]
      return {
        transactionId,
        checkoutRequestId: 'ws_CO_mock',
        merchantRequestId: 'mr_mock',
        customerMessage: done ? 'Payment completed' : 'Waiting for confirmation',
        status: done ? 'COMPLETED' : 'PENDING',
        amount: last?.type === 'TOPUP' ? last.amount : 1000,
        phoneNumber: '254712345678',
        mpesaReceipt: done ? 'MOCK' + Date.now().toString().slice(-6) : null,
        resultCode: done ? '0' : null,
        resultDesc: done ? 'The service request is processed successfully.' : null,
        callbackReceived: done,
        walletCredited: done,
        updatedAt: new Date().toISOString(),
      }
    }
    const { data } = await api.get<ApiResponse<StkPushResponse>>(`/wallet/topup/${transactionId}`)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to check payment status')
    return data.data
  }

  async checkTopup(transactionId: string): Promise<StkPushResponse> {
    if (isMockMode()) {
      return this.getTopupStatus(transactionId)
    }
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
    if (isMockMode()) {
      await delay(300)
      let items = [...mockTransactions]
      if (params.type) items = items.filter((t) => t.type === params.type)
      return toPage(items, params.page ?? 0, params.size ?? 20)
    }
    const { data } = await api.get<ApiResponse<Page<WalletTransaction>>>('/wallet/transactions', {
      params,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load transactions')
    return data.data
  }

  getPaymentInstructions(accountRef = 'YOUR-ACCOUNT'): PaymentInstructions {
    if (isMockMode()) return mockPaymentInstructions
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
