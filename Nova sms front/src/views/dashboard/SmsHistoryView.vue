<script setup lang="ts">
import { onMounted } from 'vue'
import { useSmsStore } from '@/stores/sms.store'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import FormField from '@/components/common/FormField.vue'
import AppButton from '@/components/common/AppButton.vue'
import DataTable from '@/components/tables/DataTable.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import { formatCurrency, formatDate, formatProviderError } from '@/utils/format'

const sms = useSmsStore()

onMounted(async () => {
  await Promise.all([sms.fetchHistory(), sms.fetchSenderIds()])
})

async function applyFilters() {
  await sms.fetchHistory()
}
</script>

<template>
  <div>
    <PageHeader
      title="SMS History"
      description="Search and filter outbound messages across your organization."
    />

    <AppCard class="mb-6" title="Filters">
      <div class="grid gap-4 md:grid-cols-4">
        <FormField label="Search">
          <AppInput v-model="sms.filters.search" placeholder="Phone or message…" />
        </FormField>
        <FormField label="Status">
          <AppSelect v-model="sms.filters.status" placeholder="All statuses">
            <option value="">All</option>
            <option value="PENDING">PENDING</option>
            <option value="SCHEDULED">SCHEDULED</option>
            <option value="DELIVERED">DELIVERED</option>
            <option value="FAILED">FAILED</option>
          </AppSelect>
        </FormField>
        <FormField label="Sender ID">
          <AppSelect v-model="sms.filters.senderId" placeholder="All senders">
            <option value="">All</option>
            <option v-for="s in sms.senderIds" :key="s.id" :value="s.senderName">
              {{ s.senderName }}
            </option>
          </AppSelect>
        </FormField>
        <div class="flex items-end">
          <AppButton @click="applyFilters">Apply filters</AppButton>
        </div>
      </div>
    </AppCard>

    <DataTable
      :columns="[
        { key: 'recipient', label: 'Recipient' },
        { key: 'sender', label: 'Sender ID' },
        { key: 'message', label: 'Message' },
        { key: 'cost', label: 'Cost' },
        { key: 'status', label: 'Status' },
        { key: 'date', label: 'Date' },
      ]"
    >
      <tr v-for="row in sms.history" :key="row.id" class="hover:bg-slate-50/70">
        <td class="whitespace-nowrap px-4 py-3 font-mono text-xs">{{ row.recipient }}</td>
        <td class="whitespace-nowrap px-4 py-3">{{ row.senderId }}</td>
        <td class="max-w-sm px-4 py-3 text-slate-600">
          <p class="truncate">{{ row.content }}</p>
          <p
            v-if="row.status === 'FAILED' && row.failureReason"
            class="mt-1 text-xs text-rose-600"
            :title="row.failureReason"
          >
            {{ formatProviderError(row.failureReason) }}
          </p>
        </td>
        <td class="whitespace-nowrap px-4 py-3">{{ formatCurrency(row.cost) }}</td>
        <td class="px-4 py-3"><EntityStatusBadge :status="row.status" /></td>
        <td class="whitespace-nowrap px-4 py-3 text-slate-500">
          <template v-if="row.status === 'SCHEDULED' && row.scheduledAt">
            <span class="block text-xs text-brand-700">Sends {{ formatDate(row.scheduledAt) }}</span>
            <span class="text-[11px] text-slate-400">Created {{ formatDate(row.createdAt) }}</span>
          </template>
          <template v-else>
            {{ formatDate(row.createdAt) }}
          </template>
        </td>
      </tr>
    </DataTable>
  </div>
</template>
