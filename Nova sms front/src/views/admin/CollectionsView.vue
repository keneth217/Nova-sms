<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { organizationService } from '@/api/organization.service'
import type { PaybillCollectionDashboard } from '@/models/collection.model'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import DataTable from '@/components/tables/DataTable.vue'
import { formatCurrency, formatDate, formatNumber } from '@/utils/format'
import { BanknotesIcon, CalendarDaysIcon, ClockIcon, HashtagIcon } from '@heroicons/vue/24/outline'

const dashboard = ref<PaybillCollectionDashboard | null>(null)
const selectedAccount = ref('')
const loading = ref(false)
const error = ref('')

const accounts = computed(() => dashboard.value?.accounts ?? ['SHEILA', 'KENETH'])

onMounted(() => load())

async function load(billRef = selectedAccount.value) {
  loading.value = true
  error.value = ''
  try {
    dashboard.value = await organizationService.getCollections({
      billRef: billRef || undefined,
      size: 50,
    })
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load collections'
  } finally {
    loading.value = false
  }
}

async function selectAccount(billRef: string) {
  selectedAccount.value = billRef
  await load(billRef)
}
</script>

<template>
  <div>
    <PageHeader
      title="Paybill collections"
      :description="`Deposits to Paybill ${dashboard?.paybill || '5687394'}. Use account Keneth, or account Sheila — never both. These never credit a Nova SMS wallet.`"
    />

    <p v-if="error" class="mb-4 text-sm text-rose-600">{{ error }}</p>
    <p v-else-if="loading && !dashboard" class="mb-4 text-sm text-slate-500">Loading collections…</p>

    <div class="mb-4 flex flex-wrap gap-2">
      <button
        type="button"
        class="rounded-lg px-3 py-1.5 text-sm font-medium transition"
        :class="
          selectedAccount === ''
            ? 'bg-brand-50 text-brand-700'
            : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
        "
        @click="selectAccount('')"
      >
        All accounts
      </button>
      <button
        v-for="account in accounts"
        :key="account"
        type="button"
        class="rounded-lg px-3 py-1.5 text-sm font-medium transition"
        :class="
          selectedAccount === account
            ? 'bg-brand-50 text-brand-700'
            : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
        "
        @click="selectAccount(account)"
      >
        {{ account }}
      </button>
    </div>

    <div class="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <StatCard
        label="Today"
        :value="formatCurrency(dashboard?.todayAmount ?? 0)"
        :hint="`${formatNumber(dashboard?.todayCount ?? 0)} payments`"
        :icon="ClockIcon"
        tone="brand"
      />
      <StatCard
        label="This month"
        :value="formatCurrency(dashboard?.monthAmount ?? 0)"
        :hint="`${formatNumber(dashboard?.monthCount ?? 0)} payments`"
        :icon="CalendarDaysIcon"
        tone="success"
      />
      <StatCard
        label="All time"
        :value="formatCurrency(dashboard?.totalAmount ?? 0)"
        :hint="`${formatNumber(dashboard?.totalCount ?? 0)} payments`"
        :icon="BanknotesIcon"
      />
      <StatCard
        label="Accounts"
        :value="formatNumber(dashboard?.byAccount?.length ?? accounts.length)"
        hint="Stats only — no wallet credit"
        :icon="HashtagIcon"
      />
    </div>

    <div
      v-if="dashboard?.byAccount?.length"
      class="mb-6 grid gap-4 sm:grid-cols-2"
    >
      <div
        v-for="stat in dashboard.byAccount"
        :key="stat.billRef"
        class="rounded-xl border border-slate-200/80 bg-white p-4 shadow-sm shadow-slate-900/5"
      >
        <p class="font-mono text-sm font-semibold text-slate-900">{{ stat.billRef }}</p>
        <p class="mt-2 text-xl font-semibold text-slate-900">{{ formatCurrency(stat.amount) }}</p>
        <p class="mt-1 text-xs text-slate-500">{{ formatNumber(stat.count) }} payments</p>
      </div>
    </div>

    <DataTable
      :columns="[
        { key: 'account', label: 'Account' },
        { key: 'amount', label: 'Amount' },
        { key: 'receipt', label: 'Receipt' },
        { key: 'payer', label: 'Payer' },
        { key: 'phone', label: 'MSISDN' },
        { key: 'time', label: 'Time' },
      ]"
      empty-title="No collections yet"
      empty-hint="Pay Paybill with account Keneth, or with account Sheila. Money stays in the Paybill; totals appear here."
    >
      <tr v-for="tx in dashboard?.recent?.content || []" :key="tx.id">
        <td class="px-4 py-3 font-mono text-xs font-semibold">{{ tx.billRef }}</td>
        <td class="px-4 py-3 font-medium">{{ formatCurrency(tx.amount) }}</td>
        <td class="px-4 py-3 font-mono text-xs">{{ tx.mpesaReceipt }}</td>
        <td class="px-4 py-3 text-slate-600">{{ tx.payerName || '—' }}</td>
        <td class="max-w-[140px] truncate px-4 py-3 font-mono text-xs text-slate-500">
          {{ tx.phoneNumber || '—' }}
        </td>
        <td class="whitespace-nowrap px-4 py-3 text-slate-500">{{ formatDate(tx.createdAt) }}</td>
      </tr>
    </DataTable>
  </div>
</template>
