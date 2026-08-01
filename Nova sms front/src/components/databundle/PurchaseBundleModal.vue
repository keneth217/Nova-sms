<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { BundlePaymentMode, DataBundleOffer } from '@/models/databundle.model'
import AppModal from '@/components/common/AppModal.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import FormField from '@/components/common/FormField.vue'
import { formatCurrency } from '@/utils/format'

export type PurchaseConfirmPayload = {
  paymentMode: BundlePaymentMode
  paymentPhoneNumber?: string
}

const props = defineProps<{
  open: boolean
  offer: DataBundleOffer | null
  phoneNumber: string
  loading?: boolean
  error?: string | null
}>()

const emit = defineEmits<{
  close: []
  confirm: [payload: PurchaseConfirmPayload]
}>()

const paymentMode = ref<BundlePaymentMode>('airtime')
const useOtherPaymentPhone = ref(false)
const paymentPhoneInput = ref('')

watch(
  () => props.open,
  (open) => {
    if (open) {
      paymentMode.value = 'airtime'
      useOtherPaymentPhone.value = false
      paymentPhoneInput.value = props.phoneNumber || ''
    }
  },
)

watch(paymentMode, (mode) => {
  if (mode !== 'm-pesa') {
    useOtherPaymentPhone.value = false
  } else if (!paymentPhoneInput.value) {
    paymentPhoneInput.value = props.phoneNumber || ''
  }
})

const title = computed(() =>
  props.offer ? `Buy ${props.offer.offerName}` : 'Confirm purchase',
)

const canConfirm = computed(() => {
  if (paymentMode.value !== 'm-pesa' || !useOtherPaymentPhone.value) return true
  return paymentPhoneInput.value.trim().length >= 9
})

function submit() {
  const payload: PurchaseConfirmPayload = { paymentMode: paymentMode.value }
  if (paymentMode.value === 'm-pesa' && useOtherPaymentPhone.value && paymentPhoneInput.value.trim()) {
    payload.paymentPhoneNumber = paymentPhoneInput.value.trim()
  }
  emit('confirm', payload)
}
</script>

<template>
  <AppModal :open="open" :title="title" @close="emit('close')">
    <div v-if="offer" class="space-y-4 text-sm text-slate-600">
      <p>
        Purchase <strong class="text-slate-900">{{ offer.offerName }}</strong> for
        <strong class="text-slate-900">{{ phoneNumber }}</strong>.
      </p>
      <dl class="grid grid-cols-2 gap-3 rounded-xl bg-slate-50 p-4">
        <div>
          <dt class="text-xs uppercase tracking-wide text-slate-400">Price</dt>
          <dd class="mt-1 font-semibold text-slate-900">{{ formatCurrency(Number(offer.amount)) }}</dd>
        </div>
        <div>
          <dt class="text-xs uppercase tracking-wide text-slate-400">Validity</dt>
          <dd class="mt-1 font-semibold text-slate-900">{{ offer.validity || '—' }}</dd>
        </div>
        <div v-if="offer.resourceAmount && offer.resourceAmount !== '0'" class="col-span-2">
          <dt class="text-xs uppercase tracking-wide text-slate-400">Data</dt>
          <dd class="mt-1 font-semibold text-slate-900">{{ offer.resourceAmount }} MB</dd>
        </div>
      </dl>

      <FormField label="Pay with" required>
        <AppSelect v-model="paymentMode">
          <option value="airtime">Airtime</option>
          <option value="m-pesa">M-Pesa</option>
        </AppSelect>
      </FormField>

      <div v-if="paymentMode === 'm-pesa'" class="space-y-3 rounded-xl border border-slate-200 p-3">
        <label class="flex cursor-pointer items-start gap-2 text-sm text-slate-700">
          <input
            v-model="useOtherPaymentPhone"
            type="checkbox"
            class="mt-1 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
          />
          <span>
            Pay from a different M-Pesa number
            <span class="mt-0.5 block text-xs font-normal text-slate-500">
              M-Pesa STK is sent to this number. Use the same line as the bundle recipient when
              possible for best results.
            </span>
          </span>
        </label>

        <FormField v-if="useOtherPaymentPhone" label="M-Pesa payment number" required>
          <AppInput
            v-model="paymentPhoneInput"
            type="tel"
            placeholder="0712345678"
            :disabled="loading"
          />
        </FormField>
      </div>

      <p class="text-xs text-slate-500">
        <template v-if="paymentMode === 'airtime'">
          Safaricom deducts airtime from {{ phoneNumber }}.
        </template>
        <template v-else-if="useOtherPaymentPhone">
          M-Pesa prompt goes to the payment number. Purchase may stay pending until Safaricom confirms.
        </template>
        <template v-else>
          Safaricom charges M-Pesa on {{ phoneNumber }}. Purchase may stay pending until Safaricom
          confirms.
        </template>
      </p>

      <p v-if="error" class="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{{ error }}</p>
    </div>
    <template #footer>
      <AppButton variant="secondary" :disabled="loading" @click="emit('close')">Cancel</AppButton>
      <AppButton :loading="loading" :disabled="!canConfirm" @click="submit">
        Confirm purchase
      </AppButton>
    </template>
  </AppModal>
</template>
