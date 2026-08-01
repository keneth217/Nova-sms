<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useDataBundleStore } from '@/stores/databundle.store'
import type { BundleStatus } from '@/models/databundle.model'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import FormField from '@/components/common/FormField.vue'
import DataTable from '@/components/tables/DataTable.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import { formatCurrency, formatDate } from '@/utils/format'
import { exportToCsv } from '@/utils/exportCsv'

const store = useDataBundleStore()
const status = ref<BundleStatus | ''>('')

onMounted(async () => {
  await Promise.all([store.loadHistory(), store.loadMetrics()])
})

watch(status, async (value) => {
  await store.loadHistory(value)
})

async function exportHistory() {
  const rows = store.history.map((tx) => ({
    reference: tx.reference,
    phoneNumber: tx.phoneNumber,
    offerName: tx.offerName,
    amount: tx.amount,
    status: tx.status,
    createdAt: tx.createdAt,
    failureReason: tx.failureReason || '',
  }))
  exportToCsv(rows, 'data-bundle-history.csv')
}
</script>

<template>
  <div>
    <PageHeader
      title="Bundle history"
      description="Search and export Safaricom data-bundle purchases for this organization."
    >
      <template #actions>
        <button
          type="button"
          class="rounded-lg bg-white px-3 py-2 text-sm font-medium text-slate-700 ring-1 ring-slate-200 hover:bg-slate-50"
          :disabled="!store.history.length"
          @click="exportHistory"
        >
          Export CSV
        </button>
      </template>
    </PageHeader>

    <div class="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <AppCard>
        <p class="text-xs uppercase tracking-wide text-slate-400">Sold</p>
        <p class="mt-2 text-2xl font-semibold text-slate-900">{{ store.metrics?.totalSold ?? 0 }}</p>
      </AppCard>
      <AppCard>
        <p class="text-xs uppercase tracking-wide text-slate-400">Revenue</p>
        <p class="mt-2 text-2xl font-semibold text-brand-700">
          {{ formatCurrency(Number(store.metrics?.revenue ?? 0)) }}
        </p>
      </AppCard>
      <AppCard>
        <p class="text-xs uppercase tracking-wide text-slate-400">Successful</p>
        <p class="mt-2 text-2xl font-semibold text-emerald-700">{{ store.metrics?.successful ?? 0 }}</p>
      </AppCard>
      <AppCard>
        <p class="text-xs uppercase tracking-wide text-slate-400">Failed</p>
        <p class="mt-2 text-2xl font-semibold text-rose-600">{{ store.metrics?.failed ?? 0 }}</p>
      </AppCard>
    </div>

    <AppCard title="Transactions" :padding="false">
      <div class="flex flex-wrap gap-3 border-b border-slate-100 px-5 py-4">
        <FormField label="Status" class="w-48">
          <AppSelect v-model="status">
            <option value="">All</option>
            <option value="PENDING">PENDING</option>
            <option value="SUCCESS">SUCCESS</option>
            <option value="FAILED">FAILED</option>
            <option value="CANCELLED">CANCELLED</option>
          </AppSelect>
        </FormField>
      </div>

      <DataTable
        :columns="[
          { key: 'reference', label: 'Reference' },
          { key: 'phone', label: 'Phone' },
          { key: 'offer', label: 'Offer' },
          { key: 'amount', label: 'Amount' },
          { key: 'status', label: 'Status' },
          { key: 'date', label: 'Date' },
        ]"
        empty-title="No bundle purchases yet"
      >
        <tr v-for="tx in store.history" :key="tx.id" class="hover:bg-slate-50/70">
          <td class="px-4 py-3 font-mono text-xs">{{ tx.reference }}</td>
          <td class="px-4 py-3 font-mono text-xs">{{ tx.phoneNumber }}</td>
          <td class="px-4 py-3">{{ tx.offerName }}</td>
          <td class="px-4 py-3">{{ formatCurrency(Number(tx.amount)) }}</td>
          <td class="px-4 py-3"><EntityStatusBadge :status="tx.status" /></td>
          <td class="px-4 py-3 text-slate-500">{{ formatDate(tx.createdAt) }}</td>
        </tr>
      </DataTable>
    </AppCard>
  </div>
</template>
