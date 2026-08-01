<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth.store'
import { useOrganizationStore } from '@/stores/organization.store'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const auth = useAuthStore()
const org = useOrganizationStore()

const apiBase = import.meta.env.VITE_API_BASE_URL

const displayOrgName = computed(() => {
  if (auth.isSuperAdmin) return 'Novastack Platform'
  return (
    org.currentOrganization?.name ||
    org.organizationName ||
    auth.user?.organizationName ||
    '—'
  )
})

onMounted(async () => {
  if (!auth.isSuperAdmin && auth.user?.organizationId) {
    try {
      await org.fetchCurrentOrganization()
    } catch {
      if (auth.user?.organizationName) {
        org.setOrganizationName(auth.user.organizationName)
      }
    }
  }
})
</script>

<template>
  <div>
    <PageHeader
      title="Settings"
      description="Organization details, notifications, and integration preferences."
    />

    <p v-if="org.error && !auth.isSuperAdmin" class="mb-4 text-sm text-rose-600">{{ org.error }}</p>

    <div class="grid gap-6 lg:grid-cols-2">
      <AppCard title="Organization">
        <dl class="space-y-4 text-sm">
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Name</dt>
            <dd class="font-medium text-slate-900">{{ displayOrgName }}</dd>
          </div>
          <div
            v-if="!auth.isSuperAdmin && org.currentOrganization"
            class="flex justify-between gap-4 border-b border-slate-100 pb-3"
          >
            <dt class="text-slate-500">Phone</dt>
            <dd class="font-medium text-slate-900">{{ org.currentOrganization.phone || '—' }}</dd>
          </div>
          <div
            v-if="!auth.isSuperAdmin && org.currentOrganization"
            class="flex justify-between gap-4 border-b border-slate-100 pb-3"
          >
            <dt class="text-slate-500">Org email</dt>
            <dd class="font-medium text-slate-900">{{ org.currentOrganization.email || '—' }}</dd>
          </div>
          <div
            v-if="!auth.isSuperAdmin && (org.currentOrganization?.accountType || auth.user?.accountType)"
            class="flex justify-between gap-4 border-b border-slate-100 pb-3"
          >
            <dt class="text-slate-500">Account type</dt>
            <dd>
              <StatusBadge variant="info">
                {{ org.currentOrganization?.accountType || auth.user?.accountType }}
              </StatusBadge>
            </dd>
          </div>
          <div
            v-if="!auth.isSuperAdmin && org.currentOrganization?.mpesaAccountRef"
            class="flex justify-between gap-4 border-b border-slate-100 pb-3"
          >
            <dt class="text-slate-500">M-Pesa account</dt>
            <dd class="font-mono text-xs text-slate-600">
              {{ org.currentOrganization.mpesaAccountRef }}
            </dd>
          </div>
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Organization ID</dt>
            <dd class="font-mono text-xs text-slate-600">
              {{ auth.user?.organizationId || '—' }}
            </dd>
          </div>
          <div
            v-if="!auth.isSuperAdmin && auth.user?.expiresAt"
            class="flex justify-between gap-4 border-b border-slate-100 pb-3"
          >
            <dt class="text-slate-500">Expires</dt>
            <dd class="font-medium text-slate-900">{{ formatDate(auth.user.expiresAt) }}</dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-slate-500">API base</dt>
            <dd class="font-mono text-xs text-slate-600">
              {{ apiBase }}
            </dd>
          </div>
        </dl>
      </AppCard>

      <AppCard title="Notifications">
        <div class="space-y-3 text-sm text-slate-600">
          <label class="flex items-center gap-3">
            <input type="checkbox" checked class="rounded border-slate-300 text-brand-600" />
            Email me when wallet balance falls below KES 1,000
          </label>
          <label class="flex items-center gap-3">
            <input type="checkbox" checked class="rounded border-slate-300 text-brand-600" />
            Notify when a sender ID is approved or rejected
          </label>
          <label class="flex items-center gap-3">
            <input type="checkbox" class="rounded border-slate-300 text-brand-600" />
            Weekly usage digest
          </label>
        </div>
      </AppCard>
    </div>
  </div>
</template>
