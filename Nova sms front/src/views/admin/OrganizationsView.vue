<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useOrganizationStore } from '@/stores/organization.store'
import { apiClientService } from '@/api/api-client.service'
import type { OrganizationBillingModel } from '@/models/api-client.model'
import type { OrganizationStatus } from '@/models/organization.model'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import FormField from '@/components/common/FormField.vue'
import AppModal from '@/components/common/AppModal.vue'
import DataTable from '@/components/tables/DataTable.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import { formatCurrency, formatDate, formatNumber } from '@/utils/format'

const org = useOrganizationStore()
const search = ref('')
const status = ref<OrganizationStatus | ''>('')
const showCreate = ref(false)
const showCredit = ref(false)
const saving = ref(false)
const formError = ref('')
const message = ref('')
const creditOrgId = ref('')
const creditAmount = ref(0)
const createForm = reactive({
  name: '',
  email: '',
  phone: '',
  billingModel: 'PREPAID' as OrganizationBillingModel,
  adminFullName: '',
  adminPassword: '',
  initialCredit: 0,
})

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

async function createOrganization() {
  formError.value = ''
  if (!createForm.name.trim() || !createForm.email.trim() || !createForm.phone.trim()) {
    formError.value = 'Name, email, and phone are required.'
    return
  }
  saving.value = true
  try {
    await apiClientService.createOrganization({
      name: createForm.name.trim(),
      email: createForm.email.trim(),
      phone: createForm.phone.trim(),
      billingModel: createForm.billingModel,
      adminFullName: createForm.adminFullName || undefined,
      adminPassword: createForm.adminPassword || undefined,
      initialCredit: createForm.initialCredit || undefined,
    })
    showCreate.value = false
    message.value = 'Organization created.'
    await load()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : 'Failed to create organization'
  } finally {
    saving.value = false
  }
}

function openCredit(id: string) {
  creditOrgId.value = id
  creditAmount.value = 0
  showCredit.value = true
}

async function creditWallet() {
  if (!creditOrgId.value || creditAmount.value <= 0) return
  saving.value = true
  try {
    await apiClientService.creditWallet(creditOrgId.value, creditAmount.value, 'Admin allocation')
    showCredit.value = false
    message.value = 'Wallet credited.'
    await load()
  } catch (e) {
    message.value = e instanceof Error ? e.message : 'Failed to credit wallet'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <PageHeader
      title="Organizations"
      description="Manage tenant accounts, status, and wallet visibility across the platform."
    >
      <template #actions>
        <AppButton @click="showCreate = true">New organization</AppButton>
      </template>
    </PageHeader>

    <p v-if="message" class="mb-4 text-sm text-emerald-700">{{ message }}</p>

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
        { key: 'billing', label: 'Billing' },
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
        <td class="px-4 py-3">
          <EntityStatusBadge :status="row.billingModel || 'PREPAID'" />
        </td>
        <td class="px-4 py-3"><EntityStatusBadge :status="row.status" /></td>
        <td class="px-4 py-3">{{ formatNumber(row.userCount) }}</td>
        <td class="px-4 py-3">{{ formatCurrency(row.walletBalance, row.currency) }}</td>
        <td class="px-4 py-3 text-slate-500">
          {{ row.accountType === 'EVENT' ? formatDate(row.expiresAt) : '—' }}
        </td>
        <td class="px-4 py-3">
          <div class="flex flex-wrap gap-2">
            <AppButton size="sm" variant="ghost" @click="openCredit(row.id)">Credit</AppButton>
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

    <AppModal :open="showCreate" title="Create organization" @close="showCreate = false">
      <form id="create-org" class="space-y-4" @submit.prevent="createOrganization">
        <FormField label="Name" required>
          <AppInput v-model="createForm.name" placeholder="Mwalimu" />
        </FormField>
        <FormField label="Email" required>
          <AppInput v-model="createForm.email" type="email" />
          <p class="mt-1 text-xs text-slate-500">
            Must be unused. If you set an admin password, this becomes the org admin login.
          </p>
        </FormField>
        <FormField label="Phone" required>
          <AppInput v-model="createForm.phone" type="tel" />
        </FormField>
        <FormField label="How they fund the wallet">
          <AppSelect v-model="createForm.billingModel">
            <option value="PREPAID">Prepaid — customer tops up (M-Pesa), then spends</option>
            <option value="MONTHLY">Monthly — you credit the wallet on a schedule</option>
            <option value="INTERNAL">Internal — Novastack app; you fund the wallet</option>
          </AppSelect>
          <p class="mt-1 text-xs text-slate-500">
            Sending always deducts from this organization's wallet. Choose how money gets in, not whether they use the wallet.
          </p>
        </FormField>
        <FormField label="Opening credit (KES)">
          <AppInput v-model.number="createForm.initialCredit" type="number" min="0" />
        </FormField>
        <FormField label="Admin name (optional)">
          <AppInput v-model="createForm.adminFullName" />
        </FormField>
        <FormField label="Admin password (optional)">
          <AppInput v-model="createForm.adminPassword" type="password" />
        </FormField>
        <p v-if="formError" class="text-sm text-rose-600">{{ formError }}</p>
      </form>
      <template #footer>
        <AppButton variant="secondary" :disabled="saving" @click="showCreate = false">Cancel</AppButton>
        <AppButton type="submit" form="create-org" :loading="saving">Create</AppButton>
      </template>
    </AppModal>

    <AppModal :open="showCredit" title="Credit wallet" @close="showCredit = false">
      <FormField label="Amount (KES)">
        <AppInput v-model.number="creditAmount" type="number" min="1" />
      </FormField>
      <template #footer>
        <AppButton variant="secondary" :disabled="saving" @click="showCredit = false">Cancel</AppButton>
        <AppButton :loading="saving" :disabled="creditAmount <= 0" @click="creditWallet">Credit</AppButton>
      </template>
    </AppModal>
  </div>
</template>
