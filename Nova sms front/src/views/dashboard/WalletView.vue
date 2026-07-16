<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useWalletStore } from '@/stores/wallet.store'
import type { TopupStatus } from '@/models/wallet.model'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import FormField from '@/components/common/FormField.vue'
import DataTable from '@/components/tables/DataTable.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import MpesaStkPanel from '@/components/dashboard/MpesaStkPanel.vue'
import { formatCurrency, formatDate } from '@/utils/format'

const wallet = useWalletStore()
const message = ref('')
const stkActive = ref(false)
const waitingStatus = ref<TopupStatus | 'WAITING' | 'EXPIRED' | 'SUCCESS'>('WAITING')
const waitingAmount = ref(0)
const waitingPhone = ref('')

const form = reactive({
  amount: '1000',
  phoneNumber: '0712345678',
})

let pollTimer: ReturnType<typeof setInterval> | null = null

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function refreshWallet() {
  await Promise.all([wallet.fetchBalance(), wallet.fetchTransactions()])
}

onMounted(async () => {
  await refreshWallet()
})

onUnmounted(stopPolling)

async function submitTopup() {
  message.value = ''
  try {
    const result = await wallet.topUp({
      amount: Number(form.amount),
      phoneNumber: form.phoneNumber,
    })
    waitingAmount.value = result.amount
    waitingPhone.value = result.phoneNumber
    waitingStatus.value = 'WAITING'
    stkActive.value = true
    startPolling(result.transactionId)
  } catch (e) {
    message.value = e instanceof Error ? e.message : 'Top-up failed'
  }
}

function startPolling(transactionId: string) {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const status = await wallet.pollTopupStatus(transactionId)
      if (status.status === 'COMPLETED') {
        waitingStatus.value = 'SUCCESS'
        stopPolling()
        await refreshWallet()
      } else if (status.status === 'FAILED') {
        waitingStatus.value = 'FAILED'
        stopPolling()
      }
    } catch {
    }
  }, 3000)
}

function onWaitingTimeout() {
  if (waitingStatus.value === 'WAITING') {
    waitingStatus.value = 'EXPIRED'
    stopPolling()
  }
}

function closeStkPanel() {
  const wasSuccess = waitingStatus.value === 'SUCCESS'
  stkActive.value = false
  stopPolling()
  waitingStatus.value = 'WAITING'
  if (wasSuccess) {
    message.value = 'Wallet topped up successfully.'
  }
}

function cancelWaiting() {
  stkActive.value = false
  stopPolling()
  waitingStatus.value = 'WAITING'
  message.value = 'Payment cancelled. If you already paid, the wallet will update shortly.'
}
</script>

<template>
  <div>
    <PageHeader
      title="Wallet"
      description="Monitor balance, fund via M-Pesa, and review transaction history."
    />

    <div class="grid gap-6 lg:grid-cols-3">
      <AppCard
        class="lg:col-span-1"
        :title="stkActive ? 'M-Pesa STK payment' : 'Current balance'"
      >
        <template v-if="!stkActive">
          <p class="text-3xl font-semibold tracking-tight text-slate-900">
            {{ formatCurrency(wallet.formattedBalance, wallet.currency) }}
          </p>
          <p class="mt-2 text-sm text-slate-500">
            SMS unit cost:
            <span class="font-medium text-slate-700">
              {{ formatCurrency(wallet.smsCost, wallet.currency) }}
            </span>
          </p>

          <form class="mt-6 space-y-3" @submit.prevent="submitTopup">
            <FormField label="Top-up amount (KES)" required>
              <AppInput v-model="form.amount" type="number" />
            </FormField>
            <FormField label="M-Pesa phone" required hint="07… or 254… is fine">
              <AppInput v-model="form.phoneNumber" type="tel" placeholder="0712345678" />
            </FormField>
            <AppButton type="submit" block :loading="wallet.loading">Pay with M-Pesa STK</AppButton>
            <p v-if="message" class="text-sm text-brand-700">{{ message }}</p>
          </form>
        </template>

        <MpesaStkPanel
          v-else
          :key="`${waitingPhone}-${waitingAmount}-${waitingStatus === 'WAITING'}`"
          :phone-number="waitingPhone"
          :amount="waitingAmount"
          :currency="wallet.currency"
          :transaction="wallet.lastTopup"
          :status="waitingStatus"
          :timeout-seconds="60"
          @timeout="onWaitingTimeout"
          @cancel="cancelWaiting"
          @close="closeStkPanel"
        />
      </AppCard>

      <AppCard
        class="lg:col-span-2"
        title="M-Pesa Paybill instructions"
        subtitle="Manual top-up using Lipa na M-Pesa"
      >
        <dl class="grid gap-4 sm:grid-cols-3">
          <div class="rounded-lg bg-slate-50 p-4">
            <dt class="text-xs font-medium uppercase tracking-wide text-slate-400">Paybill</dt>
            <dd class="mt-1 font-mono text-lg font-semibold text-slate-900">
              {{ wallet.paymentInstructions?.paybill }}
            </dd>
          </div>
          <div class="rounded-lg bg-slate-50 p-4">
            <dt class="text-xs font-medium uppercase tracking-wide text-slate-400">Account</dt>
            <dd class="mt-1 font-mono text-lg font-semibold text-slate-900">
              {{ wallet.paymentInstructions?.accountNumber }}
            </dd>
          </div>
          <div class="rounded-lg bg-slate-50 p-4">
            <dt class="text-xs font-medium uppercase tracking-wide text-slate-400">Business</dt>
            <dd class="mt-1 text-lg font-semibold text-slate-900">
              {{ wallet.paymentInstructions?.businessName }}
            </dd>
          </div>
        </dl>
        <ul class="mt-4 space-y-2 text-sm text-slate-600">
          <li
            v-for="(note, i) in wallet.paymentInstructions?.notes || []"
            :key="i"
            class="flex gap-2"
          >
            <span class="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" />
            {{ note }}
          </li>
        </ul>
      </AppCard>
    </div>

    <div class="mt-6">
      <AppCard
        title="Wallet transactions"
        subtitle="Top-ups, SMS debits, and adjustments"
        :padding="false"
      >
        <DataTable
          :columns="[
            { key: 'date', label: 'Date' },
            { key: 'type', label: 'Type' },
            { key: 'amount', label: 'Amount' },
            { key: 'balance', label: 'Balance after' },
            { key: 'ref', label: 'Reference' },
            { key: 'status', label: 'Status' },
          ]"
        >
          <tr v-for="tx in wallet.transactions" :key="tx.id" class="hover:bg-slate-50/70">
            <td class="whitespace-nowrap px-4 py-3 text-slate-500">
              {{ formatDate(tx.createdAt) }}
            </td>
            <td class="px-4 py-3"><EntityStatusBadge :status="tx.type" /></td>
            <td
              class="whitespace-nowrap px-4 py-3 font-medium"
              :class="tx.amount >= 0 ? 'text-emerald-700' : 'text-slate-800'"
            >
              {{ formatCurrency(tx.amount) }}
            </td>
            <td class="whitespace-nowrap px-4 py-3 text-slate-700">
              {{ formatCurrency(tx.balanceAfter) }}
            </td>
            <td class="max-w-[180px] truncate px-4 py-3 font-mono text-xs text-slate-500">
              {{ tx.mpesaReceipt || tx.reference || '—' }}
            </td>
            <td class="px-4 py-3">
              <EntityStatusBadge v-if="tx.status" :status="tx.status" />
              <span v-else class="text-slate-400">—</span>
            </td>
          </tr>
        </DataTable>
      </AppCard>
    </div>
  </div>
</template>
