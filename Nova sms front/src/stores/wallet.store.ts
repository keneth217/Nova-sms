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
import { useAuthStore } from '@/stores/auth.store'

export const useWalletStore = defineStore('wallet', () => {
  const balance = ref<WalletBalance | null>(null)
  const transactions = ref<WalletTransaction[]>([])
  const lastTopup = ref<StkPushResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const formattedBalance = computed(() => balance.value?.balance ?? 0)
  const smsCost = computed(() => balance.value?.smsCost ?? 1)
  const availableSms = computed(() => balance.value?.availableSms ?? Math.floor(formattedBalance.value / smsCost.value))
  const currency = computed(() => balance.value?.currency ?? 'KES')
  const DEFAULT_PAYBILL = '5687394'

  const paymentInstructions = computed<PaymentInstructions>(() => ({
    paybill: balance.value?.paybill || DEFAULT_PAYBILL,
    accountNumber: balance.value?.accountNumber || '',
    businessName: balance.value?.businessName || 'Novastack SMS',
    notes: [],
  }))

  async function fetchBalance() {
    if (useAuthStore().isSuperAdmin) return
    loading.value = true
    error.value = null
    try {
      balance.value = await walletService.getBalance()
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load wallet'
    } finally {
      loading.value = false
    }
  }

  async function fetchTransactions(page = 0, size = 20) {
    if (useAuthStore().isSuperAdmin) return
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
    const status = await walletService.checkTopup(transactionId)
    lastTopup.value = status
    return status
  }

  async function verifyReceipt(mpesaReceipt: string) {
    return walletService.verifyReceipt(mpesaReceipt)
  }

  async function recoverTopup(transactionId: string) {
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
    availableSms,
    currency,
    fetchBalance,
    fetchTransactions,
    topUp,
    pollTopupStatus,
    verifyReceipt,
    recoverTopup,
  }
})