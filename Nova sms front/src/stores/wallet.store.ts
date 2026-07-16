import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type {
  PaymentInstructions,
  StkPushResponse,
  WalletBalance,
  WalletTopupRequest,
  WalletTransaction,
} from '@/models/wallet.model'
import { walletService } from '@/api/wallet.service'

export const useWalletStore = defineStore('wallet', () => {
  const balance = ref<WalletBalance | null>(null)
  const transactions = ref<WalletTransaction[]>([])
  const paymentInstructions = ref<PaymentInstructions | null>(null)
  const lastTopup = ref<StkPushResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const formattedBalance = computed(() => balance.value?.balance ?? 0)
  const smsCost = computed(() => balance.value?.smsCost ?? 0.8)
  const currency = computed(() => balance.value?.currency ?? 'KES')

  async function fetchBalance() {
    loading.value = true
    error.value = null
    try {
      balance.value = await walletService.getBalance()
      paymentInstructions.value = walletService.getPaymentInstructions()
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load wallet'
    } finally {
      loading.value = false
    }
  }

  async function fetchTransactions(page = 0, size = 20) {
    loading.value = true
    error.value = null
    try {
      const result = await walletService.getTransactions({ page, size })
      transactions.value = result.content
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load transactions'
    } finally {
      loading.value = false
    }
  }

  async function topUp(payload: WalletTopupRequest) {
    loading.value = true
    error.value = null
    try {
      lastTopup.value = await walletService.topUp(payload)
      return lastTopup.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Top-up failed'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function pollTopupStatus(transactionId: string) {
    const status = await walletService.getTopupStatus(transactionId)
    lastTopup.value = status
    return status
  }

  return {
    balance,
    transactions,
    paymentInstructions,
    lastTopup,
    loading,
    error,
    formattedBalance,
    smsCost,
    currency,
    fetchBalance,
    fetchTransactions,
    topUp,
    pollTopupStatus,
  }
})