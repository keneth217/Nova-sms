<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  BuildingOffice2Icon,
  IdentificationIcon,
  PaperAirplaneIcon,
  UsersIcon,
  BanknotesIcon,
  ShieldCheckIcon,
  SignalIcon,
  CurrencyDollarIcon,
} from '@heroicons/vue/24/outline'
import { useOrganizationStore } from '@/stores/organization.store'
import { organizationService } from '@/api/organization.service'
import type { PlatformBilling, TalkSasaAccount } from '@/models/organization.model'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import DataTable from '@/components/tables/DataTable.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import { formatCurrency, formatDate, formatNumber } from '@/utils/format'

const org = useOrganizationStore()
const talksasa = ref<TalkSasaAccount | null>(null)
const talksasaError = ref('')
const billing = ref<PlatformBilling | null>(null)
const billingError = ref('')
const billingMessage = ref('')
const billingSaving = ref(false)
const billingForm = reactive({
  customerSmsPrice: '1.00',
  providerCost: '0.35',
  currency: 'KES',
})

onMounted(async () => {
  await Promise.all([
    org.fetchOverview(),
    org.fetchOrganizations({ size: 10 }),
    organizationService
      .getTalkSasaAccount()
      .then((account) => {
        talksasa.value = account
      })
      .catch((e) => {
        talksasaError.value = e instanceof Error ? e.message : 'Failed to load TalkSasa account'
      }),
    organizationService
      .getBilling()
      .then((data) => {
        billing.value = data
        billingForm.customerSmsPrice = Number(data.customerSmsPrice).toFixed(2)
        billingForm.providerCost = Number(data.providerCost).toFixed(2)
        billingForm.currency = data.currency || 'KES'
      })
      .catch((e) => {
        billingError.value = e instanceof Error ? e.message : 'Failed to load billing'
      }),
  ])
})

function remainingLabel(account: TalkSasaAccount | null) {
  const remaining = account?.balance?.remainingUnits
  if (remaining == null) return '—'
  return formatNumber(Number(remaining))
}

async function saveBilling() {
  billingMessage.value = ''
  billingError.value = ''
  billingSaving.value = true
  try {
    const updated = await organizationService.updateBilling({
      customerSmsPrice: Number(billingForm.customerSmsPrice),
      providerCost: Number(billingForm.providerCost),
      currency: billingForm.currency,
    })
    billing.value = updated
    billingMessage.value = 'Billing settings saved.'
  } catch (e) {
    billingError.value = e instanceof Error ? e.message : 'Failed to save billing'
  } finally {
    billingSaving.value = false
  }
}
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
      <AppCard
        title="SMS billing"
        subtitle="Customer price is charged to organization wallets. Provider cost is internal TalkSasa accounting only."
      >
        <p v-if="billingError" class="mb-3 text-sm text-rose-600">{{ billingError }}</p>
        <p v-else-if="!billing" class="mb-3 text-sm text-slate-500">Loading billing…</p>
        <template v-else>
          <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            <StatCard
              label="SMS provider"
              :value="billing.provider || 'TALKSASA'"
              :hint="`Default sender ${billing.defaultSenderId || 'TALK-SASA'}`"
              :icon="SignalIcon"
              tone="success"
            />
            <StatCard
              label="Customer SMS price"
              :value="formatCurrency(billing.customerSmsPrice, billing.currency)"
              hint="Charged to organization wallets"
              :icon="BanknotesIcon"
              tone="brand"
            />
            <StatCard
              label="Provider cost"
              :value="formatCurrency(billing.providerCost, billing.currency)"
              hint="Estimated TalkSasa cost per SMS unit"
              :icon="CurrencyDollarIcon"
            />
            <StatCard
              label="Gross margin"
              :value="formatCurrency(billing.grossMargin, billing.currency)"
              hint="Customer price − provider cost"
              :icon="CurrencyDollarIcon"
              tone="success"
            />
            <StatCard
              label="Total SMS units"
              :value="formatNumber(billing.totalSmsUnits)"
              :hint="`${formatNumber(billing.totalSmsSent)} billed SMS`"
              :icon="PaperAirplaneIcon"
            />
            <StatCard
              label="Customer revenue"
              :value="formatCurrency(billing.totalCustomerRevenue, billing.currency)"
              hint="SMS units × customer price"
              :icon="BanknotesIcon"
              tone="brand"
            />
            <StatCard
              label="Estimated provider cost"
              :value="formatCurrency(billing.totalEstimatedProviderCost, billing.currency)"
              hint="SMS units × 0.35"
              :icon="CurrencyDollarIcon"
            />
            <StatCard
              label="Total gross margin"
              :value="formatCurrency(billing.totalGrossMargin, billing.currency)"
              hint="Revenue − estimated provider cost"
              :icon="CurrencyDollarIcon"
              tone="success"
            />
          </div>

          <form class="mt-6 grid gap-3 sm:grid-cols-4" @submit.prevent="saveBilling">
            <label class="text-sm text-slate-600">
              Customer price
              <AppInput v-model="billingForm.customerSmsPrice" type="number" step="0.01" min="0.01" />
            </label>
            <label class="text-sm text-slate-600">
              Provider cost
              <AppInput v-model="billingForm.providerCost" type="number" step="0.01" min="0" />
            </label>
            <label class="text-sm text-slate-600">
              Currency
              <AppInput v-model="billingForm.currency" maxlength="3" />
            </label>
            <div class="flex items-end">
              <AppButton type="submit" :loading="billingSaving">Save billing</AppButton>
            </div>
          </form>
          <p v-if="billingMessage" class="mt-2 text-sm text-brand-700">{{ billingMessage }}</p>
        </template>
      </AppCard>
    </div>

    <div class="mt-6">
      <AppCard
        title="TalkSasa provider"
        subtitle="Platform TalkSasa account — not organization wallets"
      >
        <p v-if="talksasaError" class="text-sm text-rose-600">{{ talksasaError }}</p>
        <p v-else-if="!talksasa" class="text-sm text-slate-500">Loading TalkSasa account…</p>
        <div v-else class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard
            label="TalkSasa SMS units"
            :value="remainingLabel(talksasa)"
            :hint="
              talksasa.reachable
                ? talksasa.profile?.name || talksasa.profile?.email || 'Connected'
                : talksasa.errorMessage || 'Unreachable'
            "
            :icon="SignalIcon"
            :tone="talksasa.reachable ? 'success' : 'danger'"
          />
          <div class="sm:col-span-1 xl:col-span-3 text-sm text-slate-600">
            <p>
              Status:
              <span class="font-medium text-slate-900">
                {{
                  !talksasa.configured
                    ? 'Not configured'
                    : talksasa.reachable
                      ? 'Connected'
                      : 'Unreachable'
                }}
              </span>
            </p>
            <p v-if="talksasa.profile?.email" class="mt-1">
              Account: {{ talksasa.profile.email }}
            </p>
            <p v-if="talksasa.balance?.usedUnits != null" class="mt-1">
              Used {{ formatNumber(Number(talksasa.balance.usedUnits)) }}
              <span v-if="talksasa.balance.totalUnits != null">
                of {{ formatNumber(Number(talksasa.balance.totalUnits)) }}
              </span>
            </p>
            <p class="mt-2 text-xs text-slate-400">
              Customer wallets stay on Nova. This is the upstream TalkSasa credit used to deliver SMS.
            </p>
          </div>
        </div>
      </AppCard>
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
