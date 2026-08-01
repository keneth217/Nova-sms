<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useDataBundleStore } from '@/stores/databundle.store'
import type { DataBundleOffer } from '@/models/databundle.model'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import FormField from '@/components/common/FormField.vue'
import BundleOffers from '@/components/databundle/BundleOffers.vue'
import PurchaseBundleModal, {
  type PurchaseConfirmPayload,
} from '@/components/databundle/PurchaseBundleModal.vue'

const store = useDataBundleStore()
const phoneInput = ref('')
const selectedOffer = ref<DataBundleOffer | null>(null)
const showPurchaseModal = ref(false)

const hasOffers = computed(() => store.offers.length > 0)

onMounted(() => {
  store.clearMessages()
})

async function discoverOffers() {
  store.clearMessages()
  try {
    await store.loadOffers(phoneInput.value)
  } catch {
    // error shown via store
  }
}

function openPurchase(offer: DataBundleOffer) {
  store.clearMessages()
  selectedOffer.value = offer
  showPurchaseModal.value = true
}

async function confirmPurchase(payload: PurchaseConfirmPayload) {
  if (!selectedOffer.value) return
  try {
    await store.purchase(
      selectedOffer.value.offerId,
      undefined,
      payload.paymentMode,
      payload.paymentPhoneNumber,
      {
        accountId: selectedOffer.value.accountId,
        amount: selectedOffer.value.amount,
        resourceAmount: selectedOffer.value.resourceAmount,
      },
    )
    showPurchaseModal.value = false
    selectedOffer.value = null
  } catch {
    // keep modal open so user can read the error and retry
  }
}
</script>

<template>
  <div class="mx-auto max-w-5xl px-4 pb-16 pt-28 sm:px-6 lg:px-8">
    <div class="mb-8 text-center sm:text-left">
      <p class="text-sm font-semibold uppercase tracking-wide text-brand-700">Safaricom</p>
      <h1 class="mt-2 font-serif text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
        Data bundles
      </h1>
      <p class="mt-3 max-w-2xl text-slate-600">
        Enter a Safaricom number (07… or 011…) to browse data offers — no account required.
      </p>
    </div>

    <div class="grid gap-6">
      <AppCard title="Find offers">
        <form class="flex flex-col gap-4 sm:flex-row sm:items-end" @submit.prevent="discoverOffers">
          <FormField class="min-w-0 flex-1" label="Safaricom phone number" required>
            <AppInput
              v-model="phoneInput"
              type="tel"
              placeholder="0712345678 or 0117979906"
              :disabled="store.loadingOffers"
            />
          </FormField>
          <AppButton type="submit" :loading="store.loadingOffers" :disabled="!phoneInput.trim()">
            Fetch offers
          </AppButton>
        </form>
        <p v-if="store.success" class="mt-3 text-sm text-emerald-700">{{ store.success }}</p>
        <p v-if="store.error" class="mt-3 text-sm text-rose-600">{{ store.error }}</p>
        <p
          v-if="store.lastPurchase"
          class="mt-3 rounded-lg bg-slate-50 px-3 py-2 font-mono text-xs text-slate-700"
        >
          Reference {{ store.lastPurchase.reference }} · {{ store.lastPurchase.status }}
        </p>
      </AppCard>

      <AppCard v-if="hasOffers" title="Available bundles">
        <BundleOffers
          :groups="store.offersByCategory"
          :purchasing="store.purchasing"
          @buy="openPurchase"
        />
      </AppCard>
    </div>

    <PurchaseBundleModal
      :open="showPurchaseModal"
      :offer="selectedOffer"
      :phone-number="store.phoneNumber || phoneInput"
      :loading="store.purchasing"
      :error="showPurchaseModal ? store.error : null"
      @close="showPurchaseModal = false"
      @confirm="confirmPurchase"
    />
  </div>
</template>
