<script setup lang="ts">
import { onMounted } from 'vue'
import { useReportStore } from '@/stores/report.store'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import StatCard from '@/components/common/StatCard.vue'
import DataTable from '@/components/tables/DataTable.vue'
import SmsUsageChart from '@/components/dashboard/SmsUsageChart.vue'
import { formatCurrency, formatDate, formatNumber, formatPercent } from '@/utils/format'
import {
  ChartBarIcon,
  CheckBadgeIcon,
  CurrencyDollarIcon,
  PaperAirplaneIcon,
} from '@heroicons/vue/24/outline'

const reports = useReportStore()

onMounted(() => reports.fetchDashboard())
</script>

<template>
  <div>
    <PageHeader
      title="Reports"
      description="Usage trends, delivery performance, and campaign cost analysis."
    />

    <p v-if="reports.error" class="mb-4 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
      {{ reports.error }}
    </p>
    <p v-else-if="reports.loading" class="mb-4 text-sm text-slate-500">Loading live reports…</p>

    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        label="Monthly volume"
        :value="formatNumber(reports.dashboard?.smsSentThisMonth ?? 0)"
        :icon="PaperAirplaneIcon"
      />
      <StatCard
        label="Delivery rate"
        :value="formatPercent(reports.dashboard?.deliveryRate ?? 0)"
        :icon="CheckBadgeIcon"
        tone="success"
      />
      <StatCard
        label="Cost this month"
        :value="formatCurrency(reports.dashboard?.costThisMonth ?? 0)"
        :icon="CurrencyDollarIcon"
        tone="brand"
      />
      <StatCard
        label="Failed"
        :value="formatNumber(reports.dashboard?.failedCount ?? 0)"
        :icon="ChartBarIcon"
        tone="danger"
      />
    </div>

    <div class="mt-6 grid gap-6 xl:grid-cols-2">
      <AppCard title="Daily SMS volume" subtitle="Last 14 days">
        <SmsUsageChart type="line" :daily="reports.dailyVolume" />
      </AppCard>
      <AppCard title="Monthly SMS usage" subtitle="Volume by month">
        <SmsUsageChart type="bar" :monthly="reports.monthlyUsage" />
      </AppCard>
    </div>

    <div class="mt-6 grid gap-6 xl:grid-cols-2">
      <AppCard title="Usage summary" :padding="false">
        <DataTable
          :columns="[
            { key: 'period', label: 'Period' },
            { key: 'sent', label: 'Sent' },
            { key: 'delivered', label: 'Delivered' },
            { key: 'failed', label: 'Failed' },
            { key: 'cost', label: 'Cost' },
            { key: 'rate', label: 'Delivery' },
          ]"
        >
          <tr v-for="row in reports.usageSummary" :key="row.period">
            <td class="px-4 py-3 font-medium">{{ row.period }}</td>
            <td class="px-4 py-3">{{ formatNumber(row.sent) }}</td>
            <td class="px-4 py-3">{{ formatNumber(row.delivered) }}</td>
            <td class="px-4 py-3">{{ formatNumber(row.failed) }}</td>
            <td class="px-4 py-3">{{ formatCurrency(row.cost) }}</td>
            <td class="px-4 py-3">{{ formatPercent(row.deliveryRate) }}</td>
          </tr>
        </DataTable>
      </AppCard>

      <AppCard title="Top campaigns" :padding="false">
        <DataTable
          :columns="[
            { key: 'name', label: 'Campaign' },
            { key: 'recipients', label: 'Recipients' },
            { key: 'delivered', label: 'Delivered' },
            { key: 'cost', label: 'Cost' },
            { key: 'date', label: 'Date' },
          ]"
        >
          <tr v-for="c in reports.campaigns" :key="c.id">
            <td class="px-4 py-3 font-medium">{{ c.name }}</td>
            <td class="px-4 py-3">{{ formatNumber(c.recipients) }}</td>
            <td class="px-4 py-3">{{ formatNumber(c.delivered) }}</td>
            <td class="px-4 py-3">{{ formatCurrency(c.cost) }}</td>
            <td class="px-4 py-3 text-slate-500">{{ formatDate(c.createdAt, false) }}</td>
          </tr>
        </DataTable>
      </AppCard>
    </div>
  </div>
</template>
