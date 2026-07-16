<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useOrganizationStore } from '@/stores/organization.store'
import type { OrganizationStatus } from '@/models/organization.model'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import FormField from '@/components/common/FormField.vue'
import DataTable from '@/components/tables/DataTable.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import { formatCurrency, formatDate, formatNumber } from '@/utils/format'

const org = useOrganizationStore()
const search = ref('')
const status = ref<OrganizationStatus | ''>('')

async function load() {
  await org.fetchOrganizations({
    search: search.value || undefined,
    status: status.value || undefined,
  })
}

onMounted(load)

async function setStatus(id: string, next: OrganizationStatus) {
  await org.updateStatus(id, next)
}
</script>

<template>
  <div>
    <PageHeader
      title="Organizations"
      description="Manage tenant accounts, status, and wallet visibility across the platform."
    />

    <AppCard class="mb-6" title="Filters">
      <div class="grid gap-4 md:grid-cols-3">
        <FormField label="Search">
          <AppInput v-model="search" placeholder="Name or email…" />
        </FormField>
        <FormField label="Status">
          <AppSelect v-model="status">
            <option value="">All</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="PENDING">PENDING</option>
            <option value="SUSPENDED">SUSPENDED</option>
            <option value="EXPIRED">EXPIRED</option>
          </AppSelect>
        </FormField>
        <div class="flex items-end">
          <AppButton @click="load">Apply</AppButton>
        </div>
      </div>
    </AppCard>

    <DataTable
      :columns="[
        { key: 'name', label: 'Organization' },
        { key: 'type', label: 'Type' },
        { key: 'status', label: 'Status' },
        { key: 'users', label: 'Users' },
        { key: 'balance', label: 'Wallet' },
        { key: 'expires', label: 'Expires' },
        { key: 'actions', label: 'Actions' },
      ]"
    >
      <tr v-for="row in org.organizations" :key="row.id" class="hover:bg-slate-50/70">
        <td class="px-4 py-3">
          <p class="font-medium text-slate-900">{{ row.name }}</p>
          <p class="text-xs text-slate-500">{{ row.email }}</p>
        </td>
        <td class="px-4 py-3">
          <EntityStatusBadge :status="row.accountType || 'BUSINESS'" />
        </td>
        <td class="px-4 py-3"><EntityStatusBadge :status="row.status" /></td>
        <td class="px-4 py-3">{{ formatNumber(row.userCount) }}</td>
        <td class="px-4 py-3">{{ formatCurrency(row.walletBalance, row.currency) }}</td>
        <td class="px-4 py-3 text-slate-500">
          {{ row.accountType === 'EVENT' ? formatDate(row.expiresAt) : '—' }}
        </td>
        <td class="px-4 py-3">
          <div class="flex flex-wrap gap-2">
            <AppButton
              v-if="row.status !== 'ACTIVE'"
              size="sm"
              @click="setStatus(row.id, 'ACTIVE')"
            >
              Activate
            </AppButton>
            <AppButton
              v-if="row.status === 'ACTIVE'"
              size="sm"
              variant="danger"
              @click="setStatus(row.id, 'SUSPENDED')"
            >
              Suspend
            </AppButton>
          </div>
        </td>
      </tr>
    </DataTable>
  </div>
</template>
