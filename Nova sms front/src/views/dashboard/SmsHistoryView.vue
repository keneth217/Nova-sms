<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useSmsStore } from '@/stores/sms.store'
import { useWalletStore } from '@/stores/wallet.store'
import { isBillableFailure, type MessageChannel, type SmsMessage } from '@/models/sms.model'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import FormField from '@/components/common/FormField.vue'
import AppButton from '@/components/common/AppButton.vue'
import DataTable from '@/components/tables/DataTable.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import { formatCurrency, formatDate, formatProviderError, summarizeBulkSmsResult } from '@/utils/format'

const route = useRoute()
const sms = useSmsStore()
const wallet = useWalletStore()
const channel = computed<MessageChannel>(() =>
  route.meta.channel === 'WHATSAPP' ? 'WHATSAPP' : 'SMS',
)
const isWhatsApp = computed(() => channel.value === 'WHATSAPP')
const channelLabel = computed(() => (isWhatsApp.value ? 'WhatsApp' : 'SMS'))
const resendMessage = ref('')
const resendError = ref('')
const resendingKey = ref('')

const firstFailedBatchRowIds = computed(() => {
  const seen = new Set<string>()
  const ids = new Set<string>()
  for (const row of sms.history) {
    if (!row.batchId || !isBillableFailure(row.status) || seen.has(row.batchId)) continue
    seen.add(row.batchId)
    ids.add(row.id)
  }
  return ids
})

onMounted(async () => {
  await Promise.all([sms.fetchHistory(0, 20, channel.value), sms.fetchSenderIds()])
})

watch(channel, () => {
  void sms.fetchHistory(0, 20, channel.value)
})

async function applyFilters() {
  await sms.fetchHistory(0, 20, channel.value)
}

function confirmResendFailed(row: SmsMessage): boolean {
  const failedInView = sms.history.filter(
    (item) => item.batchId === row.batchId && isBillableFailure(item.status),
  ).length
  return window.confirm(
    `Resend only the failed recipients in this batch${failedInView ? ` (${failedInView} on this page)` : ''}? Already sent numbers will not be messaged again.`,
  )
}

async function resendFailedBatch(row: SmsMessage) {
  if (!row.batchId || !confirmResendFailed(row)) return
  resendMessage.value = ''
  resendError.value = ''
  resendingKey.value = row.batchId
  try {
    const result = await sms.resendFailed(row.batchId, channel.value)
    const summary = summarizeBulkSmsResult(result)
    const skipped = result.skippedCount ?? 0
    resendMessage.value = skipped
      ? `${summary.text} ${skipped} already-sent recipient${skipped === 1 ? ' was' : 's were'} skipped.`
      : summary.text
    await Promise.all([sms.fetchHistory(0, 20, channel.value), wallet.fetchBalance()])
  } catch (e) {
    resendError.value = e instanceof Error ? e.message : 'Failed to resend'
  } finally {
    resendingKey.value = ''
  }
}

async function resendSingle(row: SmsMessage) {
  if (!window.confirm(`Resend to ${row.recipient} as a new ${channelLabel.value} request?`)) return
  resendMessage.value = ''
  resendError.value = ''
  resendingKey.value = row.id
  try {
    const result = await sms.resendMessage(row.id, channel.value)
    resendMessage.value = `New ${channelLabel.value} sent to ${result.recipient} (${result.status}).`
    await Promise.all([sms.fetchHistory(0, 20, channel.value), wallet.fetchBalance()])
  } catch (e) {
    resendError.value = e instanceof Error ? e.message : 'Failed to resend'
  } finally {
    resendingKey.value = ''
  }
}
</script>

<template>
  <div>
    <PageHeader
      :title="`${channelLabel} History`"
      :description="
        isWhatsApp
          ? 'Search and filter outbound WhatsApp messages across your organization.'
          : 'Search and filter outbound messages across your organization. Resend Failed sends only failed recipients in that batch.'
      "
    />

    <p v-if="resendError" class="mb-4 text-sm text-rose-600">{{ resendError }}</p>
    <p v-else-if="resendMessage" class="mb-4 text-sm text-brand-700">{{ resendMessage }}</p>

    <AppCard class="mb-6" title="Filters">
      <div class="grid gap-4 md:grid-cols-4">
        <FormField label="Search">
          <AppInput v-model="sms.filters.search" placeholder="Phone or message…" />
        </FormField>
        <FormField label="Status">
          <AppSelect v-model="sms.filters.status" placeholder="All statuses">
            <option value="">All</option>
            <option value="PENDING">PENDING</option>
            <option value="ACCEPTED">ACCEPTED</option>
            <option value="SENT">SENT</option>
            <option value="SCHEDULED">SCHEDULED</option>
            <option value="DELIVERED">DELIVERED</option>
            <option value="FAILED">FAILED</option>
            <option value="REJECTED">REJECTED</option>
            <option value="CANCELLED">CANCELLED</option>
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
        { key: 'units', label: 'Units' },
        { key: 'status', label: 'Status' },
        { key: 'date', label: 'Date' },
        { key: 'actions', label: 'Actions' },
      ]"
    >
      <tr v-for="row in sms.history" :key="row.id" class="hover:bg-slate-50/70">
        <td class="whitespace-nowrap px-4 py-3 font-mono text-xs">{{ row.recipient }}</td>
        <td class="whitespace-nowrap px-4 py-3">{{ row.senderId }}</td>
        <td class="max-w-sm px-4 py-3 text-slate-600">
          <p class="truncate">{{ row.content }}</p>
          <p
            v-if="isBillableFailure(row.status) && row.failureReason"
            class="mt-1 text-xs text-rose-600"
            :title="row.failureReason"
          >
            {{ formatProviderError(row.failureReason) }}
          </p>
        </td>
        <td class="whitespace-nowrap px-4 py-3">{{ formatCurrency(row.cost, row.currency || 'KES') }}</td>
        <td class="whitespace-nowrap px-4 py-3 text-slate-600">{{ row.smsUnits ?? '—' }}</td>
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
        <td class="whitespace-nowrap px-4 py-3">
          <AppButton
            v-if="row.batchId && firstFailedBatchRowIds.has(row.id)"
            variant="secondary"
            size="sm"
            :loading="resendingKey === row.batchId"
            @click="resendFailedBatch(row)"
          >
            Resend failed
          </AppButton>
          <AppButton
            v-else-if="!row.batchId && isBillableFailure(row.status)"
            variant="secondary"
            size="sm"
            :loading="resendingKey === row.id"
            @click="resendSingle(row)"
          >
            Resend
          </AppButton>
        </td>
      </tr>
    </DataTable>
  </div>
</template>
