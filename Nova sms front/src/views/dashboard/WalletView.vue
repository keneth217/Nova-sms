<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import {
  BuildingLibraryIcon,
  ClipboardDocumentCheckIcon,
  ClipboardDocumentIcon,
  DevicePhoneMobileIcon,
} from '@heroicons/vue/24/outline'
import { useWalletStore } from '@/stores/wallet.store'
import { useOrganizationStore } from '@/stores/organization.store'
import type { PaymentMethod, TopupStatus, WalletTransaction } from '@/models/wallet.model'
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
const org = useOrganizationStore()
const message = ref('')
const paybillMessage = ref('')
const stkActive = ref(false)
const waitingStatus = ref<TopupStatus | 'WAITING' | 'EXPIRED' | 'SUCCESS'>('WAITING')
const waitingAmount = ref(0)
const waitingPhone = ref('')
const recheckingId = ref<string | null>(null)
const fundingMethod = ref<'stk' | 'paybill'>('stk')
const checkingPaybill = ref(false)
const copied = ref('')
const receiptNumber = ref('')
const verifyingReceipt = ref(false)

const form = reactive({
  amount: '1000',
  phoneNumber: '0712345678',
})

let pollTimer: ReturnType<typeof setInterval> | null = null
let pollDelay: ReturnType<typeof setTimeout> | null = null
const pendingTransactionId = ref<string | null>(null)

const paybill = computed(() => wallet.paymentInstructions.paybill || '5687394')
const accountNumber = computed(
  () =>
    wallet.paymentInstructions.accountNumber ||
    org.currentOrganization?.mpesaAccountRef ||
    '—',
)
const intendedAmount = computed(() => Number(form.amount) || 0)

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  if (pollDelay) {
    clearTimeout(pollDelay)
    pollDelay = null
  }
}

async function refreshWallet() {
  await Promise.all([
    wallet.fetchBalance(),
    wallet.fetchTransactions(),
    org.fetchCurrentOrganization().catch(() => undefined),
  ])
}

onMounted(async () => {
  await refreshWallet()
})

onUnmounted(stopPolling)

async function submitTopup() {
  message.value = ''
  if (pendingTransactionId.value || stkActive.value) {
    message.value = 'Wait for the current M-Pesa prompt to finish before starting another payment.'
    return
  }
  try {
    const result = await wallet.topUp({
      amount: Number(form.amount),
      phoneNumber: form.phoneNumber,
    })
    pendingTransactionId.value = result.transactionId
    waitingAmount.value = result.amount
    waitingPhone.value = result.phoneNumber
    waitingStatus.value = 'WAITING'
    stkActive.value = true
    startPolling(result.transactionId)
  } catch (e) {
    message.value = e instanceof Error ? e.message : 'Top-up failed'
  }
}

async function pollOnce(transactionId: string) {
  try {
    const status = await wallet.pollTopupStatus(transactionId)
    if (status.status === 'COMPLETED' && status.walletCredited) {
      waitingStatus.value = 'SUCCESS'
      pendingTransactionId.value = null
      stopPolling()
      await wallet.fetchBalance()
      await wallet.fetchTransactions()
      if (!stkActive.value) {
        message.value = 'Wallet topped up successfully.'
      }
    } else if (status.status === 'FAILED') {
      waitingStatus.value = 'FAILED'
      pendingTransactionId.value = null
      stopPolling()
    }
  } catch {
    // Keep polling PENDING through transient errors.
  }
}

function startPolling(transactionId: string) {
  stopPolling()
  pollDelay = setTimeout(() => {
    void pollOnce(transactionId)
    pollTimer = setInterval(() => {
      void pollOnce(transactionId)
    }, 4000)
  }, 5000)
}

function onWaitingTimeout() {
  if (waitingStatus.value === 'WAITING') {
    waitingStatus.value = 'EXPIRED'
  }
}

function closeStkPanel() {
  const wasSuccess = waitingStatus.value === 'SUCCESS'
  stkActive.value = false
  waitingStatus.value = 'WAITING'
  if (wasSuccess) {
    message.value = 'Wallet topped up successfully.'
    return
  }
  if (pendingTransactionId.value) {
    message.value = 'Still waiting for M-Pesa. Do not start another STK Push until this payment finishes.'
  }
}

function cancelWaiting() {
  stkActive.value = false
  waitingStatus.value = 'WAITING'
  if (pendingTransactionId.value) {
    message.value = 'Payment prompt closed. If you already paid, the wallet will update shortly. Do not start another STK Push yet.'
    return
  }
  stopPolling()
  message.value = 'Payment cancelled. If you already paid, the wallet will update shortly.'
}

async function recheckTopup(tx: { id: string }) {
  message.value = ''
  recheckingId.value = tx.id
  try {
    const status = await wallet.pollTopupStatus(tx.id)
    if (status.status === 'COMPLETED' && status.walletCredited) {
      message.value = 'Wallet topped up successfully.'
      await refreshWallet()
    } else {
      message.value = status.resultDesc || status.customerMessage || 'Safaricom did not confirm this payment.'
      await wallet.fetchTransactions()
    }
  } catch (e) {
    message.value = e instanceof Error ? e.message : 'Failed to query Safaricom'
  } finally {
    recheckingId.value = null
  }
}

async function copyValue(value: string, label: string) {
  if (!value || value === '—') return
  try {
    await navigator.clipboard.writeText(value)
    copied.value = label
    window.setTimeout(() => {
      if (copied.value === label) copied.value = ''
    }, 1600)
  } catch {
    copied.value = ''
  }
}

async function checkPaybillPayment() {
  paybillMessage.value = ''
  const previousBalance = wallet.formattedBalance
  const previousCompleted = new Set(
    wallet.transactions
      .filter((tx) => tx.type === 'TOPUP' && tx.status === 'COMPLETED' && tx.walletCredited)
      .map((tx) => tx.id),
  )
  checkingPaybill.value = true
  try {
    await refreshWallet()
    const newlyCredited = wallet.transactions.filter(
      (tx) =>
        tx.type === 'TOPUP' &&
        tx.status === 'COMPLETED' &&
        tx.walletCredited &&
        !previousCompleted.has(tx.id),
    )
    if (newlyCredited.length > 0 || wallet.formattedBalance > previousBalance) {
      const receipt = newlyCredited[0]?.mpesaReceipt
      paybillMessage.value = receipt
        ? `Wallet updated. Receipt ${receipt}.`
        : 'Wallet updated. A payment was credited.'
    } else {
      paybillMessage.value =
        'No new Paybill credit yet. Enter the M-Pesa receipt from your SMS to check whether Nova saved it.'
    }
  } catch (e) {
    paybillMessage.value = e instanceof Error ? e.message : 'Failed to refresh wallet'
  } finally {
    checkingPaybill.value = false
  }
}

async function verifyPaybillReceipt() {
  paybillMessage.value = ''
  const receipt = receiptNumber.value.trim()
  if (receipt.length < 8) {
    paybillMessage.value = 'Enter the M-Pesa receipt from your Safaricom SMS, for example UHJA541HGH.'
    return
  }
  verifyingReceipt.value = true
  try {
    const result = await wallet.verifyReceipt(receipt)
    paybillMessage.value = result.message
    if (result.found && result.walletCredited) {
      await refreshWallet()
    }
  } catch (e) {
    paybillMessage.value = e instanceof Error ? e.message : 'Failed to verify receipt'
  } finally {
    verifyingReceipt.value = false
  }
}

function paymentMethodLabel(tx: WalletTransaction) {
  if (tx.type !== 'TOPUP') return '—'
  const method: PaymentMethod | null | undefined = tx.paymentMethod
  if (method === 'PAYBILL' || (!method && !tx.checkoutRequestId)) return 'M-Pesa Paybill'
  return 'STK Push'
}

function canRecheck(tx: WalletTransaction) {
  return tx.type === 'TOPUP' && tx.status === 'FAILED' && !tx.walletCredited && Boolean(tx.checkoutRequestId)
}
</script>

<template>
  <div>
    <PageHeader
      title="Wallet"
      description="Top up with M-Pesa STK Push or pay directly via Paybill from any phone."
    />

    <div class="grid gap-6 xl:grid-cols-3">
      <AppCard class="xl:col-span-1" title="SMS wallet">
        <p class="text-xs font-semibold uppercase tracking-wide text-slate-400">SMS balance</p>
        <p class="mt-1 text-3xl font-semibold tracking-tight text-slate-900">
          {{ formatCurrency(wallet.formattedBalance, wallet.currency) }}
        </p>
        <dl class="mt-5 space-y-3 text-sm">
          <div class="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2.5">
            <dt class="text-slate-500">Available SMS</dt>
            <dd class="font-semibold tabular-nums text-slate-900">
              {{ wallet.availableSms.toLocaleString('en-KE') }}
            </dd>
          </div>
          <div class="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2.5">
            <dt class="text-slate-500">Your SMS charge</dt>
            <dd class="font-medium text-slate-800">
              {{ formatCurrency(wallet.smsCost, wallet.currency) }} / SMS
            </dd>
          </div>
        </dl>
        <p class="mt-3 text-xs leading-relaxed text-slate-500">
          Available SMS is this wallet balance divided by your organization SMS charge. Provider cost is billed
          separately and is not shown here.
        </p>
      </AppCard>

      <AppCard class="xl:col-span-2">
        <template #header>
          <h2 class="text-sm font-semibold text-slate-900">How do you want to top up?</h2>
          <p class="mt-0.5 text-sm text-slate-500">
            STK Push is the fast automated option. Paybill works from any M-Pesa phone.
          </p>
        </template>

        <div v-if="!stkActive" class="grid gap-3 sm:grid-cols-2">
          <button
            type="button"
            class="rounded-xl border p-4 text-left transition"
            :class="
              fundingMethod === 'stk'
                ? 'border-brand-500 bg-brand-50/70 ring-1 ring-brand-500'
                : 'border-slate-200 bg-white hover:border-slate-300'
            "
            @click="fundingMethod = 'stk'"
          >
            <div class="flex items-start gap-3">
              <span class="flex h-10 w-10 items-center justify-center rounded-lg bg-white text-brand-700 shadow-sm ring-1 ring-slate-200">
                <DevicePhoneMobileIcon class="h-5 w-5" />
              </span>
              <span>
                <span class="block text-sm font-semibold text-slate-900">M-Pesa STK Push</span>
                <span class="mt-0.5 block text-xs font-medium text-brand-700">Fast &amp; Easy</span>
                <span class="mt-1 block text-xs text-slate-500">Receive an M-Pesa prompt on your phone.</span>
              </span>
            </div>
          </button>
          <button
            type="button"
            class="rounded-xl border p-4 text-left transition"
            :class="
              fundingMethod === 'paybill'
                ? 'border-brand-500 bg-brand-50/70 ring-1 ring-brand-500'
                : 'border-slate-200 bg-white hover:border-slate-300'
            "
            @click="fundingMethod = 'paybill'"
          >
            <div class="flex items-start gap-3">
              <span class="flex h-10 w-10 items-center justify-center rounded-lg bg-white text-slate-700 shadow-sm ring-1 ring-slate-200">
                <BuildingLibraryIcon class="h-5 w-5" />
              </span>
              <span>
                <span class="block text-sm font-semibold text-slate-900">Pay via Paybill</span>
                <span class="mt-0.5 block text-xs font-medium text-slate-700">Pay from any M-Pesa phone</span>
                <span class="mt-1 block text-xs text-slate-500">No STK prompt required.</span>
              </span>
            </div>
          </button>
        </div>

        <form v-if="!stkActive && fundingMethod === 'stk'" class="mt-6 space-y-3" @submit.prevent="submitTopup">
          <FormField label="Top-up amount (KES)" required>
            <AppInput v-model="form.amount" type="number" min="1" />
          </FormField>
          <FormField label="M-Pesa phone" required hint="07… or 254… is fine">
            <AppInput v-model="form.phoneNumber" type="tel" placeholder="0712345678" />
          </FormField>
          <AppButton
            type="submit"
            block
            :loading="wallet.loading"
            :disabled="Boolean(pendingTransactionId)"
          >
            Pay with M-Pesa STK
          </AppButton>
          <p v-if="message" class="text-sm text-brand-700">{{ message }}</p>
        </form>

        <MpesaStkPanel
          v-else-if="stkActive"
          :key="`${waitingPhone}-${waitingAmount}-${waitingStatus === 'WAITING'}`"
          class="mt-2"
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

        <div v-else class="mt-6">
          <FormField label="Amount to pay (KES)" hint="Enter the same amount on your phone.">
            <AppInput v-model="form.amount" type="number" min="1" />
          </FormField>

          <div class="mt-4 overflow-hidden rounded-xl border border-slate-200">
            <div class="grid gap-px bg-slate-100 sm:grid-cols-3">
              <div class="bg-white p-4">
                <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Paybill</p>
                <div class="mt-1 flex items-center justify-between gap-2">
                  <p class="font-mono text-lg font-semibold text-slate-900">{{ paybill }}</p>
                  <button
                    type="button"
                    class="rounded-md p-1.5 text-slate-500 hover:bg-slate-50 hover:text-slate-800"
                    :title="copied === 'paybill' ? 'Copied' : 'Copy Paybill'"
                    @click="copyValue(paybill, 'paybill')"
                  >
                    <ClipboardDocumentCheckIcon v-if="copied === 'paybill'" class="h-4 w-4 text-emerald-600" />
                    <ClipboardDocumentIcon v-else class="h-4 w-4" />
                  </button>
                </div>
              </div>
              <div class="bg-white p-4">
                <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Account number</p>
                <div class="mt-1 flex items-center justify-between gap-2">
                  <p class="break-all font-mono text-lg font-semibold text-slate-900">{{ accountNumber }}</p>
                  <button
                    type="button"
                    class="shrink-0 rounded-md p-1.5 text-slate-500 hover:bg-slate-50 hover:text-slate-800"
                    :title="copied === 'account' ? 'Copied' : 'Copy account number'"
                    @click="copyValue(accountNumber, 'account')"
                  >
                    <ClipboardDocumentCheckIcon v-if="copied === 'account'" class="h-4 w-4 text-emerald-600" />
                    <ClipboardDocumentIcon v-else class="h-4 w-4" />
                  </button>
                </div>
              </div>
              <div class="bg-white p-4">
                <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Amount</p>
                <p class="mt-1 text-lg font-semibold text-slate-900">
                  {{ formatCurrency(intendedAmount, wallet.currency) }}
                </p>
              </div>
            </div>
          </div>

          <ol class="mt-5 space-y-2 text-sm text-slate-600">
            <li>1. Open M-Pesa on your phone.</li>
            <li>2. Select Lipa na M-Pesa.</li>
            <li>3. Select Pay Bill.</li>
            <li>
              4. Enter Paybill Number:
              <span class="font-mono font-semibold text-slate-900">{{ paybill }}</span>
            </li>
            <li>
              5. Enter Account Number:
              <span class="font-mono font-semibold text-slate-900">{{ accountNumber }}</span>
            </li>
            <li>
              6. Enter Amount:
              <span class="font-semibold text-slate-900">{{ formatCurrency(intendedAmount, wallet.currency) }}</span>
            </li>
            <li>7. Enter your M-Pesa PIN.</li>
            <li>8. Wait for the Safaricom confirmation SMS.</li>
          </ol>

          <p class="mt-4 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-800">
            Your SMS wallet will be credited automatically after Safaricom confirms the payment.
          </p>
          <p class="mt-2 text-xs text-slate-500">
            If the wallet does not update, enter the M-Pesa receipt. If Nova still has the
            Paybill callback, it credits your wallet from that account number automatically.
          </p>

          <FormField
            class="mt-4"
            label="M-Pesa receipt"
            hint="From the confirmation SMS, for example UHJA541HGH."
          >
            <AppInput v-model="receiptNumber" placeholder="UHJA541HGH" autocomplete="off" />
          </FormField>

          <div class="mt-4 flex flex-wrap gap-3">
            <AppButton
              :loading="verifyingReceipt"
              :disabled="receiptNumber.trim().length < 8"
              @click="verifyPaybillReceipt"
            >
              Verify receipt
            </AppButton>
            <AppButton variant="secondary" :loading="checkingPaybill" @click="checkPaybillPayment">
              Refresh balance
            </AppButton>
          </div>
          <p v-if="paybillMessage" class="mt-3 text-sm text-slate-700">{{ paybillMessage }}</p>
        </div>
      </AppCard>
    </div>

    <div class="mt-6">
      <AppCard
        title="Payment history"
        subtitle="Top-ups, SMS debits, and adjustments"
        :padding="false"
      >
        <DataTable
          :columns="[
            { key: 'date', label: 'Date' },
            { key: 'method', label: 'Method' },
            { key: 'amount', label: 'Amount' },
            { key: 'phone', label: 'Phone' },
            { key: 'receipt', label: 'Receipt' },
            { key: 'ref', label: 'Reference' },
            { key: 'status', label: 'Status' },
            { key: 'action', label: '' },
          ]"
        >
          <tr v-for="tx in wallet.transactions" :key="tx.id" class="hover:bg-slate-50/70">
            <td class="whitespace-nowrap px-4 py-3 text-slate-500">
              {{ formatDate(tx.createdAt) }}
            </td>
            <td class="px-4 py-3">
              <p class="text-sm font-medium text-slate-800">{{ paymentMethodLabel(tx) }}</p>
              <p v-if="tx.type === 'TOPUP'" class="mt-0.5 font-mono text-[11px] text-slate-500">
                Paybill {{ tx.paybill || paybill }}
                <span v-if="tx.accountNumber"> · {{ tx.accountNumber }}</span>
              </p>
              <p v-else class="mt-0.5 text-[11px] text-slate-400">{{ tx.type.replace('_', ' ') }}</p>
            </td>
            <td
              class="whitespace-nowrap px-4 py-3 font-medium"
              :class="tx.amount >= 0 && tx.type === 'TOPUP' ? 'text-emerald-700' : 'text-slate-800'"
            >
              {{ formatCurrency(tx.amount) }}
            </td>
            <td class="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-600">
              {{ tx.phoneNumber || '—' }}
            </td>
            <td class="px-4 py-3">
              <span
                v-if="tx.status === 'COMPLETED' && tx.mpesaReceipt"
                class="rounded-md bg-emerald-50 px-2 py-1 font-mono text-xs font-semibold text-emerald-800"
              >
                {{ tx.mpesaReceipt }}
              </span>
              <span v-else class="font-mono text-xs text-slate-400">{{ tx.mpesaReceipt || '—' }}</span>
            </td>
            <td class="max-w-[140px] truncate px-4 py-3 font-mono text-[11px] text-slate-500" :title="tx.id">
              {{ tx.reference || tx.id }}
            </td>
            <td class="px-4 py-3">
              <EntityStatusBadge v-if="tx.status" :status="tx.status" />
              <span v-else class="text-slate-400">—</span>
            </td>
            <td class="px-4 py-3">
              <AppButton
                v-if="canRecheck(tx)"
                size="sm"
                variant="secondary"
                :loading="recheckingId === tx.id"
                @click="recheckTopup(tx)"
              >
                Recheck M-Pesa
              </AppButton>
            </td>
          </tr>
        </DataTable>
      </AppCard>
    </div>
  </div>
</template>
