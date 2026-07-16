<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth.store'
import { useOrganizationStore } from '@/stores/organization.store'
import { isMockMode } from '@/utils/format'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const auth = useAuthStore()
const org = useOrganizationStore()

const apiMode = computed(() => (isMockMode() ? 'Mock data' : 'Live API'))
const apiBase = import.meta.env.VITE_API_BASE_URL
</script>

<template>
  <div>
    <PageHeader
      title="Settings"
      description="Account profile, organization details, and integration preferences."
    />

    <div class="grid gap-6 lg:grid-cols-2">
      <AppCard title="Profile">
        <dl class="space-y-4 text-sm">
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Full name</dt>
            <dd class="font-medium text-slate-900">{{ auth.user?.fullName }}</dd>
          </div>
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Email</dt>
            <dd class="font-medium text-slate-900">{{ auth.user?.email }}</dd>
          </div>
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Role</dt>
            <dd>
              <StatusBadge variant="brand">{{ auth.user?.role }}</StatusBadge>
            </dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-slate-500">User ID</dt>
            <dd class="font-mono text-xs text-slate-600">{{ auth.user?.userId }}</dd>
          </div>
        </dl>
      </AppCard>

      <AppCard title="Organization">
        <dl class="space-y-4 text-sm">
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Name</dt>
            <dd class="font-medium text-slate-900">
              {{ auth.isSuperAdmin ? 'Novastack Platform' : org.organizationName }}
            </dd>
          </div>
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Organization ID</dt>
            <dd class="font-mono text-xs text-slate-600">
              {{ auth.user?.organizationId || '—' }}
            </dd>
          </div>
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Data mode</dt>
            <dd>
              <StatusBadge variant="info">{{ apiMode }}</StatusBadge>
            </dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-slate-500">API base</dt>
            <dd class="font-mono text-xs text-slate-600">
              {{ apiBase }}
            </dd>
          </div>
        </dl>
      </AppCard>

      <AppCard class="lg:col-span-2" title="Notifications">
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
