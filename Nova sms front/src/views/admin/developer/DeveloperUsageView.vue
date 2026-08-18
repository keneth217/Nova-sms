<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { apiClientService } from '@/api/api-client.service'
import type { ApiClient, ApiClientUsage } from '@/models/api-client.model'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import FormField from '@/components/common/FormField.vue'
import StatCard from '@/components/common/StatCard.vue'
import SmsUsageChart from '@/components/dashboard/SmsUsageChart.vue'
import { formatCurrency, formatDate, formatNumber } from '@/utils/format'
import {
  PaperAirplaneIcon,
  CheckBadgeIcon,
  ExclamationTriangleIcon,
  WalletIcon,
  ChartBarIcon,
  ClockIcon,
} from '@heroicons/vue/24/outline'

const clients = ref<ApiClient[]>([])
const clientId = ref('')
const usage = ref<ApiClientUsage | null>(null)
const loading = ref(false)
const error = ref('')

const daily = computed(() => usage.value?.daily ?? [])

async function loadClients() {
  const page = await apiClientService.listAll({ size: 100 })
  clients.value = page.content
  if (!clientId.value && clients.value[0]) {
    clientId.value = clients.value[0].id
  }
}

async function loadUsage() {
  if (!clientId.value) {
    usage.value = null
    return
  }
  loading.value = true
  error.value = ''
  try {
    usage.value = await apiClientService.usage(clientId.value)
  } catch (e) {
    usage.value = null
    error.value = e instanceof Error ? e.message : 'Failed to load usage'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    await loadClients()
    await loadUsage()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load API clients'
  }
})
</script>

<template>
  <div>
    <PageHeader
      title="API Usage"
      description="SMS attributed to an API client. HTTP request totals are not stored; these figures come from sms_messages and the client last-used timestamp."
    />

    <FormField class="mb-6 max-w-md" label="API client">
      <AppSelect v-model="clientId" @update:model-value="loadUsage">
        <option value="" disabled>Select an API client</option>
        <option v-for="client in clients" :key="client.id" :value="client.id">
          {{ client.name }} · {{ client.organizationName || 'Organization' }}
        </option>
      </AppSelect>
    </FormField>

    <p v-if="error" class="mb-4 text-sm text-rose-600">{{ error }}</p>
    <p v-else-if="loading" class="mb-4 text-sm text-slate-500">Loading usage…</p>

    <template v-if="usage">
      <div class="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="SMS sent" :value="formatNumber(usage.totalSms)" :icon="PaperAirplaneIcon" tone="brand" />
        <StatCard
          label="Successful / in-flight"
          :value="formatNumber(usage.successfulSms)"
          hint="Not FAILED, REJECTED, or CANCELLED"
          :icon="CheckBadgeIcon"
          tone="success"
        />
        <StatCard
          label="Failed SMS"
          :value="formatNumber(usage.failedSms)"
          :icon="ExclamationTriangleIcon"
          tone="danger"
        />
        <StatCard
          label="SMS units used"
          :value="formatNumber(usage.smsUnitsUsed)"
          :icon="ChartBarIcon"
        />
        <StatCard
          label="Org wallet"
          :value="formatCurrency(usage.walletBalance, usage.walletCurrency)"
          :icon="WalletIcon"
          tone="brand"
        />
        <StatCard label="SMS today" :value="formatNumber(usage.smsToday)" />
        <StatCard label="SMS this month" :value="formatNumber(usage.smsThisMonth)" />
        <StatCard
          label="Last request"
          :value="formatDate(usage.lastRequestAt)"
          hint="API client lastUsedAt"
          :icon="ClockIcon"
        />
      </div>

      <AppCard title="Last 14 days" subtitle="SMS rows created by this API client (UTC days).">
        <SmsUsageChart v-if="daily.length" type="line" :daily="daily" />
        <p v-else class="text-sm text-slate-500">No recent SMS for this client.</p>
        <p class="mt-3 text-xs text-slate-500">Last SMS: {{ formatDate(usage.lastSmsAt) }}</p>
      </AppCard>
    </template>
  </div>
</template>
