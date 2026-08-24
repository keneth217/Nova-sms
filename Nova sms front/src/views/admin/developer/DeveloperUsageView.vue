<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiClientService } from '@/api/api-client.service'
import type {
  ApiClientUsage,
  ApiClientUsageOverview,
  ApiRequestLogRow,
} from '@/models/api-client.model'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import DataTable from '@/components/tables/DataTable.vue'
import ApiUsageChart from '@/components/dashboard/ApiUsageChart.vue'
import SmsUsageChart from '@/components/dashboard/SmsUsageChart.vue'
import {
  formatCurrency,
  formatDate,
  formatNumber,
  formatPercent,
  formatRelativeTime,
} from '@/utils/format'
import {
  CheckBadgeIcon,
  ClockIcon,
  ExclamationTriangleIcon,
  WalletIcon,
} from '@heroicons/vue/24/outline'

const route = useRoute()
const router = useRouter()

const overview = ref<ApiClientUsageOverview | null>(null)
const usage = ref<ApiClientUsage | null>(null)
const logs = ref<ApiRequestLogRow[]>([])
const logPage = ref(0)
const logTotalPages = ref(0)
const logTotal = ref(0)
const loading = ref(false)
const loadingLogs = ref(false)
const error = ref('')

const selectedClientId = computed(() => {
  const value = route.query.client
  return typeof value === 'string' && value ? value : ''
})

const requestDaily = computed(() => usage.value?.requestDaily ?? [])
const dayLabels = computed(() => requestDaily.value.map((d) => d.date.slice(5)))
const topEndpoints = computed(() => usage.value?.topEndpoints ?? [])
const ranks = computed(() => overview.value?.byClientThisMonth ?? [])

function shortPath(path: string): string {
  return path.replace(/^\/api\/v1/, '') || path
}

function formatClock(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('en-KE', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(date)
}

function statusVariant(status: number): 'success' | 'warning' | 'danger' | 'neutral' {
  if (status >= 500) return 'danger'
  if (status >= 400) return 'warning'
  if (status >= 200 && status < 300) return 'success'
  return 'neutral'
}

function selectClient(id: string | null) {
  void router.replace({ query: id ? { client: id } : {} })
}

async function loadOverview() {
  overview.value = await apiClientService.usageOverview()
}

async function loadUsage(clientId: string) {
  usage.value = await apiClientService.usage(clientId)
}

async function loadLogs(clientId: string, page = 0) {
  loadingLogs.value = true
  try {
    const result = await apiClientService.requestLogs(clientId, { page, size: 30 })
    logs.value = result.content
    logPage.value = result.number
    logTotalPages.value = result.totalPages
    logTotal.value = result.totalElements
  } finally {
    loadingLogs.value = false
  }
}

async function loadSelected() {
  const clientId = selectedClientId.value
  error.value = ''
  if (!clientId) {
    usage.value = null
    logs.value = []
    loading.value = true
    try {
      await loadOverview()
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load API usage'
    } finally {
      loading.value = false
    }
    return
  }
  loading.value = true
  try {
    if (!overview.value) {
      await loadOverview()
    }
    await Promise.all([loadUsage(clientId), loadLogs(clientId, 0)])
  } catch (e) {
    usage.value = null
    logs.value = []
    error.value = e instanceof Error ? e.message : 'Failed to load API usage'
  } finally {
    loading.value = false
  }
}

onMounted(loadSelected)
watch(selectedClientId, (id, previous) => {
  if (previous === undefined) return
  void loadSelected()
})
</script>

<template>
  <div>
    <PageHeader
      :title="usage ? usage.client.name : 'API Usage'"
      :description="
        usage
          ? `${usage.client.organizationName || 'Organization'} · HTTP volume is not the same as SMS sent. Status polling is counted as API calls, not messages.`
          : 'Usage is attributed to each API client (Mwalimu, Chamaplus, POS), not only the organization. Request logs never store API keys, PINs, or request bodies.'
      "
    >
      <template v-if="usage" #actions>
        <AppButton variant="secondary" @click="selectClient(null)">All clients</AppButton>
      </template>
    </PageHeader>

    <p v-if="error" class="mb-4 text-sm text-rose-600">{{ error }}</p>
    <p v-else-if="loading" class="mb-4 text-sm text-slate-500">Loading usage…</p>

    <template v-if="!selectedClientId && overview">
      <div class="mb-6 grid gap-4 sm:grid-cols-3">
        <StatCard label="Today" :value="formatNumber(overview.requestsToday)" hint="API requests" />
        <StatCard label="This week" :value="formatNumber(overview.requestsThisWeek)" hint="Monday to now (UTC)" />
        <StatCard label="This month" :value="formatNumber(overview.requestsThisMonth)" hint="UTC calendar month" />
      </div>

      <div class="mb-6 grid gap-4 xl:grid-cols-3">
        <AppCard class="xl:col-span-2" title="API clients" subtitle="Click a client for request logs and charts.">
          <div class="grid gap-4 md:grid-cols-2">
            <button
              v-for="client in overview.clients"
              :key="client.id"
              type="button"
              class="rounded-xl border border-slate-200/80 bg-slate-50/60 p-4 text-left transition hover:border-brand-200 hover:bg-white"
              @click="selectClient(client.id)"
            >
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="font-semibold text-slate-900">{{ client.name }}</p>
                  <p class="mt-0.5 text-xs text-slate-500">{{ client.organizationName || 'Organization' }}</p>
                </div>
                <EntityStatusBadge :status="client.status" />
              </div>
              <dl class="mt-4 grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
                <dt class="text-slate-500">Requests today</dt>
                <dd class="text-right font-medium text-slate-900">{{ formatNumber(client.requestsToday) }}</dd>
                <dt class="text-slate-500">Successful</dt>
                <dd class="text-right text-emerald-700">{{ formatNumber(client.successfulToday) }}</dd>
                <dt class="text-slate-500">Failed</dt>
                <dd class="text-right text-rose-700">{{ formatNumber(client.failedToday) }}</dd>
                <dt class="text-slate-500">SMS sent today</dt>
                <dd class="text-right font-medium text-slate-900">{{ formatNumber(client.smsSent) }}</dd>
                <dt class="text-slate-500">M-Pesa requests</dt>
                <dd class="text-right font-medium text-slate-900">{{ formatNumber(client.mpesaRequestsToday) }}</dd>
                <dt class="text-slate-500">Last request</dt>
                <dd class="text-right text-slate-700">{{ formatRelativeTime(client.lastRequestAt) }}</dd>
              </dl>
            </button>
          </div>
          <p v-if="!overview.clients.length" class="text-sm text-slate-500">No API clients yet.</p>
        </AppCard>

        <AppCard title="API client usage" subtitle="Requests this month.">
          <ul v-if="ranks.length" class="divide-y divide-slate-100">
            <li v-for="row in ranks" :key="row.id" class="flex items-center justify-between gap-3 py-2.5">
              <button type="button" class="min-w-0 text-left" @click="selectClient(row.id)">
                <p class="truncate text-sm font-medium text-slate-900">{{ row.name }}</p>
                <p class="truncate text-xs text-slate-500">{{ row.organizationName || 'Organization' }}</p>
              </button>
              <p class="shrink-0 text-sm font-semibold text-slate-900">{{ formatNumber(row.requests) }}</p>
            </li>
          </ul>
          <p v-else class="text-sm text-slate-500">No client HTTP traffic this month yet.</p>
          <ApiUsageChart
            v-if="ranks.length"
            class="mt-4"
            type="bar"
            height-class="h-48"
            :labels="ranks.map((row) => row.name)"
            :datasets="[
              {
                label: 'Requests',
                data: ranks.map((row) => row.requests),
                backgroundColor: '#0d9488',
                borderColor: '#0d9488',
              },
            ]"
          />
        </AppCard>
      </div>
    </template>

    <template v-if="usage">
      <div class="mb-6 grid gap-4 sm:grid-cols-3">
        <StatCard label="Today" :value="formatNumber(usage.requestsToday)" hint="API requests" />
        <StatCard label="This week" :value="formatNumber(usage.requestsThisWeek)" />
        <StatCard label="This month" :value="formatNumber(usage.requestsThisMonth)" />
      </div>

      <div class="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Success rate"
          :value="formatPercent(usage.successRateThisMonth)"
          hint="This month"
          :icon="CheckBadgeIcon"
          tone="success"
        />
        <StatCard
          label="Average response"
          :value="usage.averageDurationMsThisMonth == null ? '—' : `${formatNumber(usage.averageDurationMsThisMonth)} ms`"
          :icon="ClockIcon"
        />
        <StatCard
          label="HTTP 4xx / 5xx"
          :value="`${formatNumber(usage.http4xxThisMonth)} / ${formatNumber(usage.http5xxThisMonth)}`"
          :icon="ExclamationTriangleIcon"
          tone="warning"
        />
        <StatCard
          label="Org wallet"
          :value="formatCurrency(usage.walletBalance, usage.walletCurrency)"
          :icon="WalletIcon"
          tone="brand"
        />
      </div>

      <AppCard class="mb-6" title="This month" subtitle="HTTP calls versus billed business events.">
        <dl class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 text-sm">
          <div class="flex justify-between gap-3 rounded-lg bg-slate-50 px-3 py-2">
            <dt class="text-slate-500">Requests</dt>
            <dd class="font-medium text-slate-900">{{ formatNumber(usage.requestsThisMonth) }}</dd>
          </div>
          <div class="flex justify-between gap-3 rounded-lg bg-slate-50 px-3 py-2">
            <dt class="text-slate-500">SMS send</dt>
            <dd class="font-medium text-slate-900">{{ formatNumber(usage.smsSendCallsThisMonth) }}</dd>
          </div>
          <div class="flex justify-between gap-3 rounded-lg bg-slate-50 px-3 py-2">
            <dt class="text-slate-500">SMS bulk</dt>
            <dd class="font-medium text-slate-900">{{ formatNumber(usage.smsBulkCallsThisMonth) }}</dd>
          </div>
          <div class="flex justify-between gap-3 rounded-lg bg-slate-50 px-3 py-2">
            <dt class="text-slate-500">M-Pesa STK</dt>
            <dd class="font-medium text-slate-900">{{ formatNumber(usage.mpesaStkCallsThisMonth) }}</dd>
          </div>
          <div class="flex justify-between gap-3 rounded-lg bg-slate-50 px-3 py-2">
            <dt class="text-slate-500">M-Pesa status</dt>
            <dd class="font-medium text-slate-900">{{ formatNumber(usage.mpesaStatusCallsThisMonth) }}</dd>
          </div>
          <div class="flex justify-between gap-3 rounded-lg bg-slate-50 px-3 py-2">
            <dt class="text-slate-500">C2B verify</dt>
            <dd class="font-medium text-slate-900">{{ formatNumber(usage.c2bVerifyCallsThisMonth) }}</dd>
          </div>
        </dl>
        <p class="mt-3 text-xs text-slate-500">
          STK initiated {{ formatNumber(usage.mpesaStkInitiated) }} · STK successful
          {{ formatNumber(usage.mpesaStkSuccessful) }} · SMS rows {{ formatNumber(usage.totalSms) }}
          ({{ formatNumber(usage.smsUnitsUsed) }} units). Last request
          {{ formatRelativeTime(usage.lastRequestAt) }}.
        </p>
      </AppCard>

      <div class="mb-6 grid gap-4 xl:grid-cols-2">
        <AppCard title="Requests over time" subtitle="Last 14 days (UTC).">
          <ApiUsageChart
            v-if="requestDaily.length"
            :labels="dayLabels"
            :datasets="[
              {
                label: 'Requests',
                data: requestDaily.map((d) => d.requests),
                borderColor: '#0d9488',
                backgroundColor: 'rgba(13, 148, 136, 0.12)',
              },
            ]"
          />
          <p v-else class="text-sm text-slate-500">No HTTP traffic for this client yet.</p>
        </AppCard>
        <AppCard title="SMS vs M-Pesa API calls" subtitle="HTTP calls, not billed SMS or receipts.">
          <ApiUsageChart
            v-if="requestDaily.length"
            :labels="dayLabels"
            :datasets="[
              {
                label: 'SMS API',
                data: requestDaily.map((d) => d.sms),
                borderColor: '#0d9488',
                backgroundColor: 'transparent',
                fill: false,
              },
              {
                label: 'M-Pesa API',
                data: requestDaily.map((d) => d.mpesa),
                borderColor: '#0284c7',
                backgroundColor: 'transparent',
                fill: false,
              },
            ]"
          />
        </AppCard>
        <AppCard title="Success vs failures">
          <ApiUsageChart
            v-if="requestDaily.length"
            :labels="dayLabels"
            :datasets="[
              {
                label: 'Success',
                data: requestDaily.map((d) => d.success),
                borderColor: '#059669',
                backgroundColor: 'transparent',
                fill: false,
              },
              {
                label: 'Failed',
                data: requestDaily.map((d) => d.failed),
                borderColor: '#e11d48',
                backgroundColor: 'transparent',
                fill: false,
              },
            ]"
          />
        </AppCard>
        <AppCard title="Average response time" subtitle="Milliseconds.">
          <ApiUsageChart
            v-if="requestDaily.length"
            :labels="dayLabels"
            :datasets="[
              {
                label: 'Avg ms',
                data: requestDaily.map((d) => d.averageDurationMs),
                borderColor: '#7c3aed',
                backgroundColor: 'rgba(124, 58, 237, 0.08)',
              },
            ]"
          />
        </AppCard>
      </div>

      <div class="mb-6 grid gap-4 xl:grid-cols-2">
        <AppCard title="Top endpoints" subtitle="This month.">
          <ApiUsageChart
            v-if="topEndpoints.length"
            type="bar"
            height-class="h-56"
            :labels="topEndpoints.map((row) => shortPath(row.path))"
            :datasets="[
              {
                label: 'Calls',
                data: topEndpoints.map((row) => row.count),
                backgroundColor: '#0d9488',
                borderColor: '#0d9488',
              },
            ]"
          />
          <p v-else class="text-sm text-slate-500">No endpoints recorded this month.</p>
        </AppCard>
        <AppCard title="SMS delivered" subtitle="Business usage from sms_messages, not HTTP volume.">
          <SmsUsageChart v-if="usage.daily.length" type="line" :daily="usage.daily" />
          <p v-else class="text-sm text-slate-500">No recent SMS for this client.</p>
          <div class="mt-4 grid grid-cols-3 gap-3 text-sm">
            <p>
              <span class="block text-xs text-slate-500">SMS today</span>
              {{ formatNumber(usage.smsToday) }}
            </p>
            <p>
              <span class="block text-xs text-slate-500">SMS this month</span>
              {{ formatNumber(usage.smsThisMonth) }}
            </p>
            <p>
              <span class="block text-xs text-slate-500">Failed SMS</span>
              {{ formatNumber(usage.failedSms) }}
            </p>
          </div>
        </AppCard>
      </div>

      <AppCard title="Requests" subtitle="Recent scoped API-key traffic. Bodies and secrets are not stored.">
        <DataTable
          :columns="[
            { key: 'time', label: 'Time' },
            { key: 'method', label: 'Method' },
            { key: 'endpoint', label: 'Endpoint' },
            { key: 'status', label: 'Status' },
            { key: 'duration', label: 'Duration' },
            { key: 'permission', label: 'Permission' },
          ]"
        >
          <tr v-if="!logs.length">
            <td colspan="6" class="px-4 py-10 text-center text-sm text-slate-500">
              No API requests logged yet. Scoped <span class="font-mono">nova_live_</span> traffic
              appears here after the first call.
            </td>
          </tr>
          <tr v-for="row in logs" :key="row.id" class="hover:bg-slate-50/70">
            <td class="whitespace-nowrap px-4 py-3 text-slate-600">
              <span class="block font-medium text-slate-900">{{ formatClock(row.createdAt) }}</span>
              <span class="text-[11px] text-slate-400">{{ formatDate(row.createdAt, false) }}</span>
            </td>
            <td class="whitespace-nowrap px-4 py-3 font-mono text-xs">{{ row.method }}</td>
            <td class="px-4 py-3">
              <p class="font-mono text-xs text-slate-800">{{ shortPath(row.path) }}</p>
              <p class="mt-0.5 text-[11px] text-slate-400">{{ row.requestId }}</p>
            </td>
            <td class="px-4 py-3">
              <StatusBadge :variant="statusVariant(row.status)">{{ row.status }}</StatusBadge>
            </td>
            <td class="whitespace-nowrap px-4 py-3 text-slate-600">{{ formatNumber(row.durationMs) }} ms</td>
            <td class="whitespace-nowrap px-4 py-3 text-xs text-slate-500">{{ row.permission || '—' }}</td>
          </tr>
        </DataTable>
        <div class="mt-4 flex items-center justify-between gap-3">
          <p class="text-xs text-slate-500">
            Page {{ logPage + 1 }}{{ logTotalPages ? ` of ${logTotalPages}` : '' }}
            · {{ formatNumber(logTotal) }} requests
          </p>
          <div class="flex gap-2">
            <AppButton
              variant="secondary"
              size="sm"
              :disabled="loadingLogs || logPage <= 0"
              @click="loadLogs(usage.client.id, logPage - 1)"
            >
              Previous
            </AppButton>
            <AppButton
              variant="secondary"
              size="sm"
              :disabled="loadingLogs || logPage + 1 >= logTotalPages"
              @click="loadLogs(usage.client.id, logPage + 1)"
            >
              Next
            </AppButton>
          </div>
        </div>
      </AppCard>
    </template>
  </div>
</template>
