<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useSmsStore } from '@/stores/sms.store'
import { smsService } from '@/api/sms.service'
import type { TalkSasaSmsList, TalkSasaSmsView } from '@/models/sms.model'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppButton from '@/components/common/AppButton.vue'
import FormField from '@/components/common/FormField.vue'
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
const talksasaPage = ref(1)
const talksasaList = ref<TalkSasaSmsList | null>(null)
const talksasaError = ref('')
const talksasaLoading = ref(false)
const lookupUid = ref('')
const lookup = ref<TalkSasaSmsView | null>(null)
const lookupError = ref('')
const lookupLoading = ref(false)

onMounted(() => {
  void sms.fetchHistory(0, 50)
  void loadTalkSasa(1)
})

const counts = () => {
  const all = sms.history
  return {
    queued: all.filter((m) =>
      m.status === 'PENDING' || m.status === 'ACCEPTED' || m.status === 'SENT' || m.status === 'SCHEDULED',
    ).length,
    delivered: all.filter((m) => m.status === 'DELIVERED').length,
    failed: all.filter((m) => m.status === 'FAILED').length,
    total: all.length,
  }
}

function formatTalkSasaDate(value?: string | null) {
  if (!value) return '—'
  const formatted = formatDate(value)
  return formatted === '—' ? value : formatted
}

async function loadTalkSasa(page: number) {
  talksasaLoading.value = true
  talksasaError.value = ''
  try {
    talksasaList.value = await smsService.listTalkSasaSms(page, 25)
    talksasaPage.value = talksasaList.value.page || page
    if (!talksasaList.value.configured || !talksasaList.value.reachable) {
      talksasaError.value = talksasaList.value.errorMessage || 'TalkSasa inbox is unavailable'
    }
  } catch (e) {
    talksasaError.value = e instanceof Error ? e.message : 'Failed to load TalkSasa SMS'
  } finally {
    talksasaLoading.value = false
  }
}

async function lookupTalkSasa() {
  const uid = lookupUid.value.trim()
  if (!uid) return
  lookupLoading.value = true
  lookupError.value = ''
  lookup.value = null
  try {
    lookup.value = await smsService.getTalkSasaSms(uid)
    if (!lookup.value.item) {
      lookupError.value = lookup.value.errorMessage || 'TalkSasa SMS not found'
    } else if (lookup.value.errorMessage) {
      lookupError.value = lookup.value.errorMessage
    }
  } catch (e) {
    lookupError.value = e instanceof Error ? e.message : 'Failed to load TalkSasa SMS'
  } finally {
    lookupLoading.value = false
  }
}

function canPage(delta: number) {
  const list = talksasaList.value
  if (!list) return false
  const next = talksasaPage.value + delta
  if (next < 1) return false
  if (list.lastPage != null) return next <= list.lastPage
  return (list.items?.length || 0) > 0 || delta < 0
}
</script>

<template>
  <div>
    <PageHeader
      title="SMS monitoring"
      description="Nova outbound history plus live TalkSasa GET /sms and GET /sms/{uid}."
    />

    <div class="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard label="Visible Nova messages" :value="formatNumber(counts().total)" :icon="SignalIcon" />
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

    <AppCard
      class="mb-6"
      title="TalkSasa inbox"
      subtitle="Live provider list for the shared TalkSasa account. Super Admin only."
    >
      <form class="mb-5 grid gap-4 md:grid-cols-[1fr_auto] md:items-end" @submit.prevent="lookupTalkSasa">
        <FormField label="View one SMS">
          <AppInput v-model="lookupUid" placeholder="TalkSasa uid or queue uid" />
        </FormField>
        <AppButton type="submit" :loading="lookupLoading">Look up</AppButton>
      </form>
      <p v-if="lookupError" class="mb-4 text-sm text-rose-600">{{ lookupError }}</p>
      <div
        v-if="lookup?.item"
        class="mb-6 rounded-lg border border-slate-200 bg-slate-50/80 px-4 py-3 text-sm"
      >
        <p class="font-mono text-xs text-slate-500">{{ lookup.item.uid }}</p>
        <p class="mt-1 font-medium text-slate-800">
          {{ lookup.item.recipient || '—' }}
          <span class="font-normal text-slate-500">via {{ lookup.item.senderId || '—' }}</span>
        </p>
        <p class="mt-1 text-slate-600">{{ lookup.item.message || '—' }}</p>
        <p class="mt-2 text-xs text-slate-500">
          {{ lookup.item.status || '—' }}
          <span v-if="lookup.item.type"> · {{ lookup.item.type }}</span>
          <span v-if="lookup.item.createdAt"> · {{ formatTalkSasaDate(lookup.item.createdAt) }}</span>
        </p>
        <p v-if="lookup.item.novaMessage" class="mt-2 text-xs text-brand-700">
          Nova {{ lookup.item.novaMessage.organizationName || 'org' }}
          · {{ lookup.item.novaMessage.status }}
          · {{ lookup.item.novaMessage.id }}
        </p>
        <p v-else class="mt-2 text-xs text-slate-400">No matching Nova SMS for this uid.</p>
      </div>

      <p v-if="talksasaError" class="mb-4 text-sm text-rose-600">{{ talksasaError }}</p>
      <p v-else-if="talksasaLoading && !talksasaList" class="mb-4 text-sm text-slate-500">
        Loading TalkSasa messages…
      </p>

      <DataTable
        :columns="[
          { key: 'uid', label: 'TalkSasa uid' },
          { key: 'recipient', label: 'Recipient' },
          { key: 'sender', label: 'Sender' },
          { key: 'message', label: 'Message' },
          { key: 'status', label: 'Status' },
          { key: 'org', label: 'Nova org' },
          { key: 'date', label: 'Date' },
        ]"
      >
        <tr
          v-for="row in talksasaList?.items || []"
          :key="row.uid || row.message || String(row.createdAt)"
          class="hover:bg-slate-50/70"
        >
          <td class="px-4 py-3 font-mono text-xs text-slate-600">{{ row.uid || '—' }}</td>
          <td class="px-4 py-3 font-mono text-xs">{{ row.recipient || '—' }}</td>
          <td class="px-4 py-3">{{ row.senderId || '—' }}</td>
          <td class="max-w-sm truncate px-4 py-3 text-slate-600">{{ row.message || '—' }}</td>
          <td class="px-4 py-3 text-slate-600">{{ row.status || '—' }}</td>
          <td class="px-4 py-3 text-slate-600">
            {{ row.novaMessage?.organizationName || '—' }}
          </td>
          <td class="px-4 py-3 text-slate-500">{{ formatTalkSasaDate(row.createdAt) }}</td>
        </tr>
      </DataTable>
      <div class="mt-4 flex items-center justify-between gap-3">
        <p class="text-xs text-slate-500">
          Page {{ talksasaList?.page || talksasaPage }}
          <span v-if="talksasaList?.lastPage"> of {{ talksasaList.lastPage }}</span>
          <span v-if="talksasaList?.total != null"> · {{ formatNumber(talksasaList.total) }} total</span>
        </p>
        <div class="flex gap-2">
          <AppButton
            variant="secondary"
            size="sm"
            :disabled="talksasaLoading || !canPage(-1)"
            @click="loadTalkSasa(talksasaPage - 1)"
          >
            Previous
          </AppButton>
          <AppButton
            variant="secondary"
            size="sm"
            :disabled="talksasaLoading || !canPage(1)"
            @click="loadTalkSasa(talksasaPage + 1)"
          >
            Next
          </AppButton>
        </div>
      </div>
    </AppCard>

    <DataTable
      :columns="[
        { key: 'organization', label: 'Organization' },
        { key: 'channel', label: 'Channel' },
        { key: 'recipient', label: 'Recipient' },
        { key: 'uid', label: 'TalkSasa uid' },
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
        <td class="px-4 py-3 text-slate-600">{{ row.channel || 'SMS' }}</td>
        <td class="px-4 py-3 font-mono text-xs">{{ row.recipient }}</td>
        <td class="px-4 py-3 font-mono text-xs text-slate-500">{{ row.providerMessageId || '—' }}</td>
        <td class="px-4 py-3">{{ row.senderId }}</td>
        <td class="max-w-sm truncate px-4 py-3 text-slate-600">{{ row.content }}</td>
        <td class="px-4 py-3">{{ formatCurrency(row.cost) }}</td>
        <td class="px-4 py-3"><EntityStatusBadge :status="row.status" /></td>
        <td class="px-4 py-3 text-slate-500">{{ formatDate(row.createdAt) }}</td>
      </tr>
    </DataTable>
  </div>
</template>
