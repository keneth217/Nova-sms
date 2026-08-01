<script setup lang="ts">
import { onMounted } from 'vue'
import { useSmsStore } from '@/stores/sms.store'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import DataTable from '@/components/tables/DataTable.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import { formatCurrency, formatDate, formatNumber } from '@/utils/format'
import {
  SignalIcon,
  CheckBadgeIcon,
  ExclamationTriangleIcon,
  QueueListIcon,
} from '@heroicons/vue/24/outline'

const sms = useSmsStore()

onMounted(() => sms.fetchHistory(0, 50))

const counts = () => {
  const all = sms.history
  return {
    queued: all.filter((m) => m.status === 'PENDING' || m.status === 'SCHEDULED').length,
    delivered: all.filter((m) => m.status === 'DELIVERED').length,
    failed: all.filter((m) => m.status === 'FAILED').length,
    total: all.length,
  }
}
</script>

<template>
  <div>
    <PageHeader
      title="SMS monitoring"
      description="Platform-wide message pipeline health and recent traffic."
    />

    <div class="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard label="Visible messages" :value="formatNumber(counts().total)" :icon="SignalIcon" />
      <StatCard
        label="Queued"
        :value="formatNumber(counts().queued)"
        :icon="QueueListIcon"
        tone="warning"
      />
      <StatCard
        label="Delivered"
        :value="formatNumber(counts().delivered)"
        :icon="CheckBadgeIcon"
        tone="success"
      />
      <StatCard
        label="Failed"
        :value="formatNumber(counts().failed)"
        :icon="ExclamationTriangleIcon"
        tone="danger"
      />
    </div>

    <DataTable
      :columns="[
        { key: 'organization', label: 'Organization' },
        { key: 'recipient', label: 'Recipient' },
        { key: 'sender', label: 'Sender ID' },
        { key: 'message', label: 'Message' },
        { key: 'cost', label: 'Cost' },
        { key: 'status', label: 'Status' },
        { key: 'date', label: 'Date' },
      ]"
    >
      <tr v-for="row in sms.history" :key="row.id" class="hover:bg-slate-50/70">
        <td class="px-4 py-3 font-medium text-slate-800">
          {{ row.organizationName || '—' }}
        </td>
        <td class="px-4 py-3 font-mono text-xs">{{ row.recipient }}</td>
        <td class="px-4 py-3">{{ row.senderId }}</td>
        <td class="max-w-sm truncate px-4 py-3 text-slate-600">{{ row.content }}</td>
        <td class="px-4 py-3">{{ formatCurrency(row.cost) }}</td>
        <td class="px-4 py-3"><EntityStatusBadge :status="row.status" /></td>
        <td class="px-4 py-3 text-slate-500">{{ formatDate(row.createdAt) }}</td>
      </tr>
    </DataTable>
  </div>
</template>
