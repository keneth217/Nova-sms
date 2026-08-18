<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import {
  WalletIcon,
  PaperAirplaneIcon,
  ChartBarIcon,
  ExclamationTriangleIcon,
  CheckBadgeIcon,
  IdentificationIcon,
  QueueListIcon,
  BanknotesIcon,
} from '@heroicons/vue/24/outline'
import { useAuthStore } from '@/stores/auth.store'
import { useReportStore } from '@/stores/report.store'
import { useSmsStore } from '@/stores/sms.store'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import AppCard from '@/components/common/AppCard.vue'
import DataTable from '@/components/tables/DataTable.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import SmsUsageChart from '@/components/dashboard/SmsUsageChart.vue'
import { formatCurrency, formatDate, formatNumber, formatPercent } from '@/utils/format'

const auth = useAuthStore()
const reports = useReportStore()
const sms = useSmsStore()

const isEventAccount = computed(() => auth.user?.accountType === 'EVENT')

const eventDaysLeft = computed(() => {
  if (!isEventAccount.value || !auth.user?.expiresAt) return null
  const left = Math.ceil((new Date(auth.user.expiresAt).getTime() - Date.now()) / 86_400_000)
  return Math.max(0, left)
})

onMounted(async () => {
  await Promise.all([reports.fetchDashboard(), sms.fetchSenderIds(), sms.fetchHistory()])
})
</script>

<template>
  <div>
    <PageHeader
      title="Dashboard"
      description="Live overview of wallet health, SMS traffic, and delivery performance."
    />

    <p v-if="reports.error" class="mb-4 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
      {{ reports.error }}
    </p>
    <p v-else-if="reports.loading" class="mb-4 text-sm text-slate-500">Loading live dashboard…</p>

    <div
      v-if="isEventAccount"
      class="mb-6 rounded-xl border border-brand-200 bg-brand-50 px-4 py-3 text-sm text-brand-900"
    >
      <p class="font-semibold">Event account (1-week access)</p>
      <p class="mt-1 text-brand-800">
        Built for events, ceremonies, and one-time sends.
        <template v-if="eventDaysLeft !== null">
          About <strong>{{ eventDaysLeft }} day{{ eventDaysLeft === 1 ? '' : 's' }}</strong> remaining
          <template v-if="auth.user?.expiresAt">
            (until {{ formatDate(auth.user.expiresAt) }})
          </template>
          .
        </template>
        Top up your wallet, then go to
        <RouterLink to="/bulk-sms" class="font-semibold underline underline-offset-2">Bulk SMS</RouterLink>
        to notify everyone.
      </p>
    </div>

    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      <StatCard
        label="Wallet balance"
        :value="formatCurrency(reports.dashboard?.walletBalance ?? 0)"
        hint="Available for outbound SMS"
        :icon="WalletIcon"
        tone="brand"
      />
      <StatCard
        label="SMS price"
        :value="`${formatCurrency(reports.dashboard?.smsPrice ?? 1)} / SMS`"
        hint="Customer charge per SMS unit"
        :icon="BanknotesIcon"
        tone="default"
      />
      <StatCard
        label="Available SMS"
        :value="formatNumber(reports.dashboard?.availableSms ?? 0)"
        hint="Wallet balance ÷ SMS price"
        :icon="QueueListIcon"
        tone="brand"
      />
      <StatCard
        label="SMS sent"
        :value="formatNumber(reports.dashboard?.smsSent ?? reports.dashboard?.smsSentThisMonth ?? 0)"
        hint="Lifetime billed messages"
        :icon="PaperAirplaneIcon"
        tone="default"
      />
      <StatCard
        label="SMS sent today"
        :value="formatNumber(reports.dashboard?.smsSentToday ?? 0)"
        :hint="`Cost ${formatCurrency(reports.dashboard?.costToday ?? 0)}`"
        :icon="PaperAirplaneIcon"
        tone="default"
      />
      <StatCard
        label="SMS this month"
        :value="formatNumber(reports.dashboard?.smsSentThisMonth ?? 0)"
        :hint="`Cost ${formatCurrency(reports.dashboard?.costThisMonth ?? 0)}`"
        :icon="ChartBarIcon"
        tone="default"
      />
      <StatCard
        label="Delivery rate"
        :value="formatPercent(reports.dashboard?.deliveryRate ?? 0)"
        :hint="`${formatNumber(reports.dashboard?.deliveredCount ?? 0)} delivered`"
        :icon="CheckBadgeIcon"
        tone="success"
      />
      <StatCard
        label="Pending / in flight"
        :value="formatNumber(reports.dashboard?.pendingCount ?? 0)"
        hint="Awaiting delivery this month"
        :icon="QueueListIcon"
        tone="warning"
      />
      <StatCard
        label="Failed messages"
        :value="formatNumber(reports.dashboard?.failedCount ?? 0)"
        hint="Month to date"
        :icon="ExclamationTriangleIcon"
        tone="danger"
      />
      <StatCard
        label="SMS units"
        :value="formatNumber(reports.dashboard?.totalSmsUnits ?? 0)"
        :hint="`Spent ${formatCurrency(reports.dashboard?.totalAmountSpent ?? reports.dashboard?.costThisMonth ?? 0)}`"
        :icon="ChartBarIcon"
        tone="default"
      />
      <StatCard
        label="Active sender IDs"
        :value="
          formatNumber(
            reports.dashboard?.activeSenderIds ??
              sms.senderIds.filter((s) => s.status === 'APPROVED').length,
          )
        "
        hint="Approved for sending"
        :icon="IdentificationIcon"
        tone="warning"
      />
    </div>

    <div class="mt-6 grid gap-6 xl:grid-cols-5">
      <AppCard
        class="xl:col-span-3"
        title="Daily SMS trend"
        subtitle="Sent vs delivered over the last 14 days"
      >
        <SmsUsageChart type="line" :daily="reports.dailyVolume" />
      </AppCard>
      <AppCard
        class="xl:col-span-2"
        title="Recent activity"
        subtitle="Latest workspace events"
        :padding="false"
      >
        <ul class="divide-y divide-slate-100">
          <li
            v-for="item in reports.activity"
            :key="item.id"
            class="flex items-start justify-between gap-3 px-5 py-3.5"
          >
            <div>
              <p class="text-sm font-medium text-slate-800">{{ item.title }}</p>
              <p class="text-xs text-slate-500">{{ item.description }}</p>
            </div>
            <div class="text-right">
              <EntityStatusBadge v-if="item.status" :status="item.status" />
              <p class="mt-1 text-[11px] text-slate-400">{{ formatDate(item.timestamp) }}</p>
            </div>
          </li>
        </ul>
      </AppCard>
    </div>

    <div class="mt-6">
      <AppCard title="Recent SMS" subtitle="Latest outbound messages" :padding="false">
        <DataTable
          :columns="[
            { key: 'recipient', label: 'Recipient' },
            { key: 'sender', label: 'Sender ID' },
            { key: 'message', label: 'Message' },
            { key: 'status', label: 'Status' },
            { key: 'date', label: 'Date' },
          ]"
        >
          <tr v-for="row in sms.history.slice(0, 5)" :key="row.id" class="hover:bg-slate-50/70">
            <td class="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-700">
              {{ row.recipient }}
            </td>
            <td class="whitespace-nowrap px-4 py-3 text-slate-700">{{ row.senderId }}</td>
            <td class="max-w-xs truncate px-4 py-3 text-slate-600">{{ row.content }}</td>
            <td class="px-4 py-3"><EntityStatusBadge :status="row.status" /></td>
            <td class="whitespace-nowrap px-4 py-3 text-slate-500">
              {{ formatDate(row.createdAt) }}
            </td>
          </tr>
        </DataTable>
      </AppCard>
    </div>
  </div>
</template>
