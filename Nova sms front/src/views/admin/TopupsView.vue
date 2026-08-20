<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { organizationService } from '@/api/organization.service'
import type { MpesaReceiptLookup, WalletTransaction } from '@/models/wallet.model'
import type { AdminOrganization } from '@/models/organization.model'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import DataTable from '@/components/tables/DataTable.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import FormField from '@/components/common/FormField.vue'
import { formatCurrency, formatDate, formatNumber } from '@/utils/format'
import { BanknotesIcon, ClockIcon, CheckCircleIcon } from '@heroicons/vue/24/outline'

const transactions = ref<WalletTransaction[]>([])
const organizations = ref<AdminOrganization[]>([])
const loading = ref(false)
const error = ref('')
const message = ref('')
const checkingId = ref<string | null>(null)
const receiptNumber = ref('')
const receiptResult = ref<MpesaReceiptLookup | null>(null)
const verifyingReceipt = ref(false)
const creditingReceipt = ref(false)
const creditAccountNumber = ref('')
const creditAmount = ref('')

const completedTopups = computed(() =>
  transactions.value.filter((t) => t.type === 'TOPUP' && t.status === 'COMPLETED'),
)
const pendingTopups = computed(() =>
  transactions.value.filter((t) => t.type === 'TOPUP' && t.status === 'PENDING'),
)

const orgNameById = computed(() => {
  const map = new Map<string, string>()
  for (const org of organizations.value) map.set(org.id, org.name)
  return map
})

onMounted(async () => {
  await load()
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [page, orgs] = await Promise.all([
      organizationService.listTopups({ size: 50 }),
      organizationService.listOrganizations({ size: 100 }),
    ])
    transactions.value = page.content
    organizations.value = orgs.content
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load top-ups'
  } finally {
    loading.value = false
  }
}

function orgName(id: string) {
  return orgNameById.value.get(id) || id
}

function canRecheck(tx: WalletTransaction) {
  return tx.status === 'FAILED' && !tx.walletCredited && Boolean(tx.checkoutRequestId)
}

async function recheck(tx: WalletTransaction) {
  message.value = ''
  error.value = ''
  checkingId.value = tx.id
  try {
    const result = await organizationService.checkTopup(tx.id)
    await load()
    if (result.status === 'COMPLETED' && result.walletCredited) {
      message.value = `Credited ${formatCurrency(result.amount)} for ${orgName(tx.organizationId)}.`
    } else if (result.status === 'FAILED') {
      message.value = result.resultDesc || 'Safaricom still reports this payment as failed.'
    } else {
      message.value = result.customerMessage || 'Still pending at Safaricom.'
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to query Safaricom'
  } finally {
    checkingId.value = null
  }
}

async function verifyReceipt() {
  message.value = ''
  error.value = ''
  receiptResult.value = null
  const receipt = receiptNumber.value.trim()
  if (receipt.length < 8) {
    error.value = 'Enter an M-Pesa receipt from the Safaricom SMS, for example UHJA541HGH.'
    return
  }
  verifyingReceipt.value = true
  try {
    receiptResult.value = await organizationService.verifyTopupReceipt(receipt)
    message.value = receiptResult.value.message
    if (receiptResult.value.walletCredited) {
      await load()
    }
    if (receiptResult.value.billRef) {
      creditAccountNumber.value = receiptResult.value.billRef
    }
    if (receiptResult.value.amount) {
      creditAmount.value = String(receiptResult.value.amount)
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to verify receipt'
  } finally {
    verifyingReceipt.value = false
  }
}

async function creditReceipt() {
  message.value = ''
  error.value = ''
  const receipt = receiptNumber.value.trim()
  if (!creditAccountNumber.value.trim() || Number(creditAmount.value) <= 0) {
    error.value = 'Enter the Paybill account number and amount from the M-Pesa SMS before crediting.'
    return
  }
  creditingReceipt.value = true
  try {
    receiptResult.value = await organizationService.creditTopupReceipt({
      mpesaReceipt: receipt,
      accountNumber: creditAccountNumber.value,
      amount: Number(creditAmount.value),
    })
    message.value = receiptResult.value.message
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to credit receipt'
  } finally {
    creditingReceipt.value = false
  }
}
</script>

<template>
  <div>
    <PageHeader
      title="Wallet funding"
      description="Monitor M-Pesa top-ups across organizations. Verify a delayed Paybill by receipt. Recheck a FAILED STK row to query Safaricom."
    />

    <p v-if="error" class="mb-4 text-sm text-rose-600">{{ error }}</p>
    <p v-else-if="message" class="mb-4 text-sm text-emerald-700">{{ message }}</p>

    <div class="mb-6 rounded-xl border border-slate-200 bg-white p-4">
      <h2 class="text-sm font-semibold text-slate-900">Verify M-Pesa receipt</h2>
      <p class="mt-1 text-sm text-slate-500">
        Enter the M-Pesa receipt first. If Nova stored the original C2B callback, it
        resolves the organization from BillRefNumber (for example NOVAC727) and credits
        that wallet. organizationId is never taken from this form. Manual account + amount
        is only required when the callback was never saved.
      </p>
      <div class="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
        <FormField class="sm:max-w-xs" label="Receipt" hint="From the Safaricom SMS">
          <AppInput v-model="receiptNumber" placeholder="UHJA541HGH" autocomplete="off" />
        </FormField>
        <AppButton :loading="verifyingReceipt" :disabled="receiptNumber.trim().length < 8" @click="verifyReceipt">
          Verify receipt
        </AppButton>
      </div>
      <div
        v-if="receiptResult && receiptResult.needsManualRecovery && !receiptResult.walletCredited"
        class="mt-4 grid gap-3 rounded-lg bg-slate-50 p-4 sm:grid-cols-3"
      >
        <FormField label="Account number" hint="From the M-Pesa SMS, e.g. NOVAC727">
          <AppInput v-model="creditAccountNumber" placeholder="NOVAC727" autocomplete="off" />
        </FormField>
        <FormField label="Amount (KES)" hint="Must match the M-Pesa SMS">
          <AppInput v-model="creditAmount" type="number" min="1" />
        </FormField>
        <div class="flex items-end">
          <AppButton
            :loading="creditingReceipt"
            :disabled="!creditAccountNumber.trim() || Number(creditAmount) <= 0"
            @click="creditReceipt"
          >
            Credit wallet
          </AppButton>
        </div>
      </div>
    </div>

    <div class="mb-6 grid gap-4 sm:grid-cols-3">
      <StatCard
        label="Completed top-ups"
        :value="formatNumber(completedTopups.length)"
        :icon="CheckCircleIcon"
        tone="success"
      />
      <StatCard
        label="Pending"
        :value="formatNumber(pendingTopups.length)"
        :icon="ClockIcon"
        tone="warning"
      />
      <StatCard
        label="Top-up volume"
        :value="formatCurrency(completedTopups.reduce((s, t) => s + t.amount, 0))"
        :icon="BanknotesIcon"
        tone="brand"
      />
    </div>

    <DataTable
      :columns="[
        { key: 'org', label: 'Organization' },
        { key: 'method', label: 'Method' },
        { key: 'amount', label: 'Amount' },
        { key: 'phone', label: 'Phone' },
        { key: 'receipt', label: 'Receipt' },
        { key: 'status', label: 'Status' },
        { key: 'date', label: 'Date' },
        { key: 'action', label: '' },
      ]"
      :empty-title="loading ? 'Loading…' : 'No top-ups yet'"
    >
      <tr
        v-for="tx in transactions.filter((t) => t.type === 'TOPUP')"
        :key="tx.id"
        class="hover:bg-slate-50/70"
      >
        <td class="px-4 py-3 font-medium">{{ tx.organizationName || orgName(tx.organizationId) }}</td>
        <td class="px-4 py-3">
          <p class="text-sm font-medium text-slate-800">
            {{ tx.paymentMethod === 'PAYBILL' || (!tx.paymentMethod && !tx.checkoutRequestId) ? 'M-Pesa Paybill' : 'STK Push' }}
          </p>
          <p class="mt-0.5 font-mono text-[11px] text-slate-500">
            {{ tx.paybill || '—' }}
            <span v-if="tx.accountNumber"> · {{ tx.accountNumber }}</span>
          </p>
        </td>
        <td class="px-4 py-3 text-emerald-700 font-semibold">{{ formatCurrency(tx.amount) }}</td>
        <td class="px-4 py-3 font-mono text-xs">{{ tx.phoneNumber || '—' }}</td>
        <td class="px-4 py-3">
          <span
            v-if="tx.status === 'COMPLETED' && tx.mpesaReceipt"
            class="rounded-md bg-emerald-50 px-2 py-1 font-mono text-xs font-semibold text-emerald-800"
          >
            {{ tx.mpesaReceipt }}
          </span>
          <span v-else class="font-mono text-xs">{{ tx.mpesaReceipt || '—' }}</span>
        </td>
        <td class="px-4 py-3">
          <EntityStatusBadge :status="tx.status || 'PENDING'" />
          <p v-if="tx.resultDesc" class="mt-1 max-w-[220px] text-xs text-slate-500">{{ tx.resultDesc }}</p>
        </td>
        <td class="px-4 py-3 text-slate-500">{{ formatDate(tx.createdAt) }}</td>
        <td class="px-4 py-3">
          <AppButton
            v-if="canRecheck(tx)"
            size="sm"
            variant="secondary"
            :loading="checkingId === tx.id"
            @click="recheck(tx)"
          >
            Recheck M-Pesa
          </AppButton>
        </td>
      </tr>
    </DataTable>
  </div>
</template>
