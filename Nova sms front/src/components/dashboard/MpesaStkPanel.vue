<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { DevicePhoneMobileIcon } from '@heroicons/vue/24/outline'
import type { StkPushResponse, TopupStatus } from '@/models/wallet.model'
import { formatCurrency } from '@/utils/format'
import AppButton from '@/components/common/AppButton.vue'

const props = withDefaults(
  defineProps<{
    phoneNumber: string
    amount: number
    currency?: string
    transaction?: StkPushResponse | null
    status?: TopupStatus | 'WAITING' | 'EXPIRED' | 'SUCCESS'
    timeoutSeconds?: number
  }>(),
  {
    currency: 'KES',
    transaction: null,
    status: 'WAITING',
    timeoutSeconds: 60,
  },
)

const emit = defineEmits<{
  close: []
  timeout: []
  cancel: []
}>()

const secondsLeft = ref(props.timeoutSeconds)
let timer: ReturnType<typeof setInterval> | null = null

const displayStatus = computed(() => {
  if (props.status === 'COMPLETED' || props.status === 'SUCCESS') return 'SUCCESS'
  if (props.status === 'FAILED') return 'FAILED'
  if (props.status === 'EXPIRED' || secondsLeft.value <= 0) return 'EXPIRED'
  return 'WAITING'
})

const heading = computed(() => {
  switch (displayStatus.value) {
    case 'SUCCESS':
      return 'Payment received'
    case 'FAILED':
      return 'Payment failed'
    case 'EXPIRED':
      return 'Request timed out'
    default:
      return 'Please confirm on your phone'
  }
})

const subtitle = computed(() => {
  switch (displayStatus.value) {
    case 'SUCCESS':
      return 'Your wallet has been credited successfully.'
    case 'FAILED':
      return props.transaction?.resultDesc || 'The M-Pesa payment could not be completed.'
    case 'EXPIRED':
      return 'Still waiting for M-Pesa. Do not start another payment until this one finishes.'
    default:
      return 'Check your phone for the M-Pesa payment request.'
  }
})

function clearTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function startTimer() {
  clearTimer()
  secondsLeft.value = props.timeoutSeconds
  timer = setInterval(() => {
    if (secondsLeft.value <= 1) {
      secondsLeft.value = 0
      clearTimer()
      emit('timeout')
      return
    }
    secondsLeft.value -= 1
  }, 1000)
}

watch(
  () => props.status,
  (status, prev) => {
    if (status === 'WAITING' && prev !== 'WAITING') {
      startTimer()
      return
    }
    if (status === 'COMPLETED' || status === 'SUCCESS' || status === 'FAILED') {
      clearTimer()
    }
  },
)

onUnmounted(clearTimer)

startTimer()
</script>

<template>
  <div class="text-center" role="status" aria-live="polite">
    <div
      v-if="displayStatus === 'WAITING'"
      class="relative mx-auto mb-6 flex h-28 w-28 items-center justify-center"
      aria-hidden="true"
    >
      <span
        v-for="i in 3"
        :key="i"
        class="mpesa-ripple absolute inset-0 rounded-full border border-[#c9b8a8]/70"
        :style="{ animationDelay: `${(i - 1) * 0.55}s` }"
      />
      <div
        class="relative z-10 flex h-14 w-14 items-center justify-center rounded-full shadow-sm"
        style="background: linear-gradient(135deg, #c8d4e8 0%, #e8d5c4 45%, #f0c4a8 100%)"
      >
        <DevicePhoneMobileIcon class="h-7 w-7 text-[#e07a3a]" stroke-width="1.5" />
      </div>
    </div>

    <div
      v-else
      class="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-full"
      :class="{
        'bg-emerald-100 text-emerald-600': displayStatus === 'SUCCESS',
        'bg-rose-100 text-rose-600': displayStatus === 'FAILED' || displayStatus === 'EXPIRED',
      }"
    >
      <svg
        v-if="displayStatus === 'SUCCESS'"
        class="h-8 w-8"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        stroke-width="2"
      >
        <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
      </svg>
      <svg
        v-else
        class="h-8 w-8"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        stroke-width="2"
      >
        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
      </svg>
    </div>

    <h3 class="text-xl font-semibold tracking-tight text-slate-900">{{ heading }}</h3>
    <p class="mt-1.5 text-sm text-slate-500">{{ subtitle }}</p>

    <div class="mt-5 overflow-hidden rounded-xl border border-slate-200 bg-white text-left">
      <div class="flex items-center justify-between border-b border-slate-100 px-4 py-3 text-sm">
        <span class="text-slate-500">Phone number</span>
        <span class="font-medium text-slate-900">{{ phoneNumber }}</span>
      </div>
      <div class="flex items-center justify-between border-b border-slate-100 px-4 py-3 text-sm">
        <span class="text-slate-500">Time remaining</span>
        <span
          class="font-medium tabular-nums"
          :class="secondsLeft <= 10 && displayStatus === 'WAITING' ? 'text-rose-600' : 'text-slate-900'"
        >
          {{ displayStatus === 'WAITING' ? `${secondsLeft}s` : '—' }}
        </span>
      </div>
      <div class="flex items-center justify-between px-4 py-3 text-sm">
        <span class="text-slate-500">Amount</span>
        <span class="font-semibold text-slate-900">
          {{ formatCurrency(amount, currency) }}
        </span>
      </div>
      <div
        v-if="transaction?.mpesaReceipt && displayStatus === 'SUCCESS'"
        class="flex items-center justify-between border-t border-slate-100 px-4 py-3 text-sm"
      >
        <span class="text-slate-500">Receipt</span>
        <span class="font-mono font-semibold text-emerald-700">{{ transaction.mpesaReceipt }}</span>
      </div>
    </div>

    <p class="mt-4 text-xs leading-relaxed text-slate-500">
      Enter your M-Pesa PIN when prompted on your phone to complete the payment.
    </p>

    <div class="mt-5 flex justify-center gap-3">
      <AppButton v-if="displayStatus === 'WAITING'" variant="secondary" block @click="emit('cancel')">
        Cancel
      </AppButton>
      <AppButton v-else block @click="emit('close')">
        {{ displayStatus === 'SUCCESS' ? 'Done' : displayStatus === 'EXPIRED' ? 'Keep waiting' : 'Try again' }}
      </AppButton>
    </div>
  </div>
</template>

<style scoped>
.mpesa-ripple {
  animation: mpesa-bulge 1.8s cubic-bezier(0.22, 0.61, 0.36, 1) infinite;
  border-color: rgba(180, 160, 145, 0.55);
}

@keyframes mpesa-bulge {
  0% {
    transform: scale(0.55);
    opacity: 0.7;
  }
  70% {
    opacity: 0.25;
  }
  100% {
    transform: scale(1.35);
    opacity: 0;
  }
}
</style>
