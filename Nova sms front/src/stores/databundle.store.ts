import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { dataBundleService } from '@/api/databundle.service'
import type {
  BundlePaymentMode,
  BundleStatus,
  DataBundleMetrics,
  DataBundleOffer,
  DataBundleTransaction,
} from '@/models/databundle.model'

export const useDataBundleStore = defineStore('dataBundles', () => {
  const phoneNumber = ref('')
  const offers = ref<DataBundleOffer[]>([])
  const history = ref<DataBundleTransaction[]>([])
  const metrics = ref<DataBundleMetrics | null>(null)
  const lastPurchase = ref<DataBundleTransaction | null>(null)
  const loadingOffers = ref(false)
  const purchasing = ref(false)
  const loadingHistory = ref(false)
  const error = ref<string | null>(null)
  const success = ref<string | null>(null)

  const offersByCategory = computed(() => {
    const groups: Record<string, DataBundleOffer[]> = {
      DAILY: [],
      WEEKLY: [],
      MONTHLY: [],
      PROMOTIONAL: [],
      OTHER: [],
    }
    for (const offer of offers.value) {
      const key = (offer.category || 'OTHER').toUpperCase()
      if (!groups[key]) groups[key] = []
      groups[key]!.push(offer)
    }
    return groups
  })

  async function loadOffers(phone: string) {
    loadingOffers.value = true
    error.value = null
    success.value = null
    try {
      const result = await dataBundleService.fetchOffers(phone)
      phoneNumber.value = result.phoneNumber
      offers.value = result.offers
      success.value = `Found ${result.offers.length} offers for ${result.phoneNumber}`
    } catch (e) {
      offers.value = []
      error.value = e instanceof Error ? e.message : 'Failed to load offers'
      throw e
    } finally {
      loadingOffers.value = false
    }
  }

  async function purchase(
    offerId: string,
    phone?: string,
    paymentMode: BundlePaymentMode = 'airtime',
    paymentPhoneNumber?: string,
    fingerprint?: Pick<DataBundleOffer, 'accountId' | 'amount' | 'resourceAmount'>,
  ) {
    purchasing.value = true
    error.value = null
    success.value = null
    try {
      // Prefer explicit fingerprint; fall back to the offer already in store (same card).
      const fromStore = offers.value.find((o) => o.offerId === offerId)
      const accountId = fingerprint?.accountId ?? fromStore?.accountId ?? undefined
      const amount = fingerprint?.amount ?? fromStore?.amount
      const resourceAmount = fingerprint?.resourceAmount ?? fromStore?.resourceAmount ?? undefined
      const tx = await dataBundleService.purchase({
        phoneNumber: phone || phoneNumber.value,
        offerId,
        accountId: accountId || undefined,
        amount: amount != null ? Number(amount) : undefined,
        resourceAmount: resourceAmount || undefined,
        paymentMode,
        paymentPhoneNumber: paymentPhoneNumber || undefined,
      })
      lastPurchase.value = tx

      // M-Pesa is async; airtime can also stay PENDING when Safaricom returns an empty body.
      if (tx.status === 'PENDING') {
        success.value =
          paymentMode === 'm-pesa'
            ? `STK prompt sent (${tx.reference}). Waiting for M-Pesa confirmation…`
            : `Purchase submitted (${tx.reference}). Confirming with Safaricom…`
        const settled = await pollPurchaseStatus(tx.reference)
        lastPurchase.value = settled
        applyPurchaseOutcome(settled)
        return settled
      }

      applyPurchaseOutcome(tx)
      return tx
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Purchase failed. Please try again.'
      throw e
    } finally {
      purchasing.value = false
    }
  }

  function applyPurchaseOutcome(tx: DataBundleTransaction) {
    if (tx.status === 'SUCCESS') {
      success.value = `Bundle activated successfully (${tx.reference}).`
      error.value = null
      return
    }
    if (tx.status === 'FAILED' || tx.status === 'CANCELLED') {
      error.value = tx.failureReason || tx.responseDescription || `Purchase failed (${tx.reference}).`
      success.value = null
      return
    }
    success.value = `Purchase submitted (${tx.reference}). Status: ${tx.status}. You will be notified when Safaricom completes it.`
  }

  /** Polls GET /data-bundles/status/{reference} which calls Safaricom Check Status when PENDING. */
  async function pollPurchaseStatus(
    reference: string,
    attempts = 12,
    intervalMs = 5000,
  ): Promise<DataBundleTransaction> {
    let latest: DataBundleTransaction | null = null
    for (let i = 0; i < attempts; i++) {
      await new Promise((r) => setTimeout(r, intervalMs))
      try {
        latest = await dataBundleService.getStatus(reference)
        lastPurchase.value = latest
        if (latest.status !== 'PENDING') {
          return latest
        }
        success.value = `Waiting for M-Pesa confirmation… (${i + 1}/${attempts})`
      } catch {
        // Keep waiting — transient status failures shouldn't kill the poll loop.
      }
    }
    return (
      latest || {
        id: '',
        reference,
        phoneNumber: phoneNumber.value,
        offerId: '',
        offerName: '',
        category: 'OTHER',
        amount: 0,
        status: 'PENDING',
        checkoutRequestId: null,
        responseCode: null,
        responseDescription: 'Still pending after status checks',
        failureReason: null,
        createdAt: new Date().toISOString(),
        updatedAt: null,
      }
    )
  }

  async function refreshStatus(reference: string) {
    error.value = null
    try {
      const tx = await dataBundleService.getStatus(reference)
      lastPurchase.value = tx
      applyPurchaseOutcome(tx)
      return tx
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to refresh status'
      throw e
    }
  }

  async function loadHistory(status: BundleStatus | '' = '') {
    loadingHistory.value = true
    try {
      const page = await dataBundleService.history({
        status: status || undefined,
        size: 50,
      })
      history.value = page.content
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load history'
    } finally {
      loadingHistory.value = false
    }
  }

  async function loadMetrics() {
    try {
      metrics.value = await dataBundleService.metrics()
    } catch {
      // metrics are optional on first paint
    }
  }

  function clearMessages() {
    error.value = null
    success.value = null
  }

  return {
    phoneNumber,
    offers,
    offersByCategory,
    history,
    metrics,
    lastPurchase,
    loadingOffers,
    purchasing,
    loadingHistory,
    error,
    success,
    loadOffers,
    purchase,
    loadHistory,
    loadMetrics,
    refreshStatus,
    clearMessages,
  }
})
