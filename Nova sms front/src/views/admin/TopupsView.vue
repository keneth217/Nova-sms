<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { organizationService } from '@/api/organization.service'
import type { WalletTransaction } from '@/models/wallet.model'
import type { AdminOrganization } from '@/models/organization.model'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import DataTable from '@/components/tables/DataTable.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import { formatCurrency, formatDate, formatNumber } from '@/utils/format'
import { BanknotesIcon, ClockIcon, CheckCircleIcon } from '@heroicons/vue/24/outline'

const transactions = ref<WalletTransaction[]>([])
const organizations = ref<AdminOrganization[]>([])
const loading = ref(false)
const error = ref('')

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
})

function orgName(id: string) {
  return orgNameById.value.get(id) || id
}
</script>

<template>
  <div>
    <PageHeader
      title="Wallet funding"
      description="Monitor M-Pesa top-ups and funding activity across organizations."
    />

    <p v-if="error" class="mb-4 text-sm text-rose-600">{{ error }}</p>

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
        { key: 'amount', label: 'Amount' },
        { key: 'phone', label: 'Phone' },
        { key: 'receipt', label: 'Receipt' },
        { key: 'status', label: 'Status' },
        { key: 'date', label: 'Date' },
      ]"
      :empty-title="loading ? 'Loading…' : 'No top-ups yet'"
    >
      <tr
        v-for="tx in transactions.filter((t) => t.type === 'TOPUP')"
        :key="tx.id"
        class="hover:bg-slate-50/70"
      >
        <td class="px-4 py-3 font-medium">{{ orgName(tx.organizationId) }}</td>
        <td class="px-4 py-3 text-emerald-700 font-semibold">{{ formatCurrency(tx.amount) }}</td>
        <td class="px-4 py-3 font-mono text-xs">{{ tx.phoneNumber || '—' }}</td>
        <td class="px-4 py-3 font-mono text-xs">{{ tx.mpesaReceipt || '—' }}</td>
        <td class="px-4 py-3">
          <EntityStatusBadge :status="tx.status || 'PENDING'" />
        </td>
        <td class="px-4 py-3 text-slate-500">{{ formatDate(tx.createdAt) }}</td>
      </tr>
    </DataTable>
  </div>
</template>
