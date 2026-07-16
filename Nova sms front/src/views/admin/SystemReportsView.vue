<script setup lang="ts">
import { onMounted } from 'vue'
import {
  BuildingOffice2Icon,
  IdentificationIcon,
  PaperAirplaneIcon,
  UsersIcon,
  BanknotesIcon,
  ShieldCheckIcon,
} from '@heroicons/vue/24/outline'
import { useOrganizationStore } from '@/stores/organization.store'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import AppCard from '@/components/common/AppCard.vue'
import DataTable from '@/components/tables/DataTable.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import { formatCurrency, formatDate, formatNumber } from '@/utils/format'

const org = useOrganizationStore()

onMounted(async () => {
  await Promise.all([org.fetchOverview(), org.fetchOrganizations({ size: 10 })])
})
</script>

<template>
  <div>
    <PageHeader
      title="System reports"
      description="Platform KPIs across organizations from live admin APIs."
    />

    <p v-if="org.error" class="mb-4 text-sm text-rose-600">{{ org.error }}</p>
    <p v-else-if="org.loading" class="mb-4 text-sm text-slate-500">Loading platform data…</p>

    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      <StatCard
        label="Organizations"
        :value="formatNumber(org.overview?.organizations ?? 0)"
        :icon="BuildingOffice2Icon"
        tone="brand"
      />
      <StatCard label="Users" :value="formatNumber(org.overview?.users ?? 0)" :icon="UsersIcon" />
      <StatCard
        label="Super admins"
        :value="formatNumber(org.overview?.superAdmins ?? 0)"
        :icon="ShieldCheckIcon"
      />
      <StatCard
        label="Total SMS sent"
        :value="formatNumber(org.overview?.totalSmsSent ?? 0)"
        :icon="PaperAirplaneIcon"
      />
      <StatCard
        label="Pending sender IDs"
        :value="formatNumber(org.overview?.pendingSenderIds ?? 0)"
        :icon="IdentificationIcon"
        tone="warning"
      />
      <StatCard
        label="Pending top-ups"
        :value="formatNumber(org.overview?.pendingTopups ?? 0)"
        :icon="BanknotesIcon"
        tone="warning"
      />
    </div>

    <div class="mt-6">
      <AppCard title="Recent organizations" subtitle="From /admin/organizations" :padding="false">
        <DataTable
          :columns="[
            { key: 'name', label: 'Organization' },
            { key: 'status', label: 'Status' },
            { key: 'balance', label: 'Wallet' },
            { key: 'created', label: 'Created' },
          ]"
        >
          <tr v-for="row in org.organizations.slice(0, 10)" :key="row.id">
            <td class="px-4 py-3 font-medium">{{ row.name }}</td>
            <td class="px-4 py-3"><EntityStatusBadge :status="row.status" /></td>
            <td class="px-4 py-3">{{ formatCurrency(row.walletBalance, row.currency) }}</td>
            <td class="px-4 py-3 text-slate-500">{{ formatDate(row.createdAt, false) }}</td>
          </tr>
        </DataTable>
      </AppCard>
    </div>
  </div>
</template>
