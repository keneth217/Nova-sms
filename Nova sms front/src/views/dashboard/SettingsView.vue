<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { useOrganizationStore } from '@/stores/organization.store'
import { organizationService } from '@/api/organization.service'
import { formatCurrency, formatDate } from '@/utils/format'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import FormField from '@/components/common/FormField.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import type { C2bCallbackUrls, PlatformNotificationSettings } from '@/models/organization.model'

const auth = useAuthStore()
const org = useOrganizationStore()

const apiBase = import.meta.env.VITE_API_BASE_URL
const notificationsEnabled = ref(false)
const threshold = ref('')
const saving = ref(false)
const savingDetails = ref(false)
const savedMessage = ref('')
const detailsMessage = ref('')
const localError = ref('')
const detailsError = ref('')
const platformSettings = ref<PlatformNotificationSettings | null>(null)
const platformError = ref('')
const c2bUrls = ref<C2bCallbackUrls | null>(null)
const c2bError = ref('')
const c2bMessage = ref('')
const registeringC2b = ref(false)
const orgForm = reactive({
  name: '',
  email: '',
  phone: '',
})

const displayOrgName = computed(() => {
  if (auth.isSuperAdmin) return 'Novastack Platform'
  return (
    org.currentOrganization?.name ||
    org.organizationName ||
    auth.user?.organizationName ||
    '—'
  )
})

const orgPhone = computed(() => org.currentOrganization?.phone || orgForm.phone || '—')

const platformNotificationsOn = computed(() => {
  if (auth.isSuperAdmin) {
    return platformSettings.value?.enabled ?? false
  }
  return org.currentOrganization?.platformNotificationsEnabled ?? true
})

const thresholdPlaceholder = computed(() => {
  const fallback =
    org.currentOrganization?.platformLowBalanceThreshold ??
    platformSettings.value?.lowBalanceThreshold
  if (fallback != null && Number.isFinite(Number(fallback))) {
    return String(fallback)
  }
  return 'Amount in KES'
})

function fillOrgForm() {
  const current = org.currentOrganization
  if (!current) return
  orgForm.name = current.name || ''
  orgForm.email = current.email || ''
  orgForm.phone = current.phone || ''
  notificationsEnabled.value = current.notificationsEnabled !== false
  if (current.lowBalanceThreshold != null && Number.isFinite(Number(current.lowBalanceThreshold))) {
    threshold.value = String(current.lowBalanceThreshold)
  }
}

watch(
  () => org.currentOrganization,
  () => fillOrgForm(),
  { immediate: true },
)

async function registerC2bUrls() {
  registeringC2b.value = true
  c2bError.value = ''
  c2bMessage.value = ''
  try {
    const result = await organizationService.registerC2bUrls()
    c2bUrls.value = result
    if (result.alreadyRegistered === 'true') {
      c2bError.value =
        result.message ||
        'Daraja already has URLs for this Paybill. Delete the Mwalimu URLs in Daraja URL management, then register again.'
      return
    }
    if (result.success === 'false') {
      c2bError.value = result.message || 'Daraja rejected the C2B URL registration.'
      return
    }
    c2bMessage.value = result.message || 'C2B URLs registered with Daraja.'
  } catch (e) {
    c2bError.value = e instanceof Error ? e.message : 'Failed to register C2B URLs'
  } finally {
    registeringC2b.value = false
  }
}

onMounted(async () => {
  if (auth.isSuperAdmin) {
    try {
      platformSettings.value = await organizationService.getNotificationSettings()
    } catch (e) {
      platformError.value = e instanceof Error ? e.message : 'Failed to load notification settings'
    }
    try {
      c2bUrls.value = await organizationService.getC2bUrls()
    } catch (e) {
      c2bError.value = e instanceof Error ? e.message : 'Failed to load C2B URLs'
    }
    return
  }
  if (auth.user?.organizationId) {
    try {
      await org.fetchCurrentOrganization()
    } catch {
      if (auth.user?.organizationName) {
        org.setOrganizationName(auth.user.organizationName)
      }
    }
  }
})

async function saveSettings() {
  localError.value = ''
  savedMessage.value = ''
  const amount = Number(threshold.value)
  if (!Number.isFinite(amount) || amount < 0) {
    localError.value = 'Enter a threshold of 0 or more.'
    return
  }
  saving.value = true
  try {
    await org.updateSettings({
      name: orgForm.name.trim() || undefined,
      email: orgForm.email.trim() || undefined,
      phone: orgForm.phone.trim() || undefined,
      notificationsEnabled: notificationsEnabled.value,
      lowBalanceThreshold: amount,
    })
    try {
      await auth.fetchProfile()
    } catch {
      void 0
    }
    savedMessage.value = notificationsEnabled.value
      ? 'Notification settings saved. We will SMS your organization phone when the wallet drops to this amount.'
      : 'Notification settings saved. Welcome, top-up, and low-balance SMS are turned off for this organization.'
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Failed to save settings'
  } finally {
    saving.value = false
  }
}

async function saveOrgDetails() {
  detailsError.value = ''
  detailsMessage.value = ''
  const name = orgForm.name.trim()
  const email = orgForm.email.trim()
  const phone = orgForm.phone.trim()
  if (!name) {
    detailsError.value = 'Enter the organization name.'
    return
  }
  if (!email) {
    detailsError.value = 'Enter the organization email.'
    return
  }
  if (!phone) {
    detailsError.value = 'Enter the organization phone number.'
    return
  }
  const amount = Number(threshold.value)
  if (!Number.isFinite(amount) || amount < 0) {
    detailsError.value = 'Set a notification threshold of 0 or more before saving details.'
    return
  }
  savingDetails.value = true
  try {
    await org.updateSettings({
      name,
      email,
      phone,
      notificationsEnabled: notificationsEnabled.value,
      lowBalanceThreshold: amount,
    })
    try {
      await auth.fetchProfile()
    } catch {
      void 0
    }
    detailsMessage.value = 'Organization details saved.'
  } catch (e) {
    detailsError.value = e instanceof Error ? e.message : 'Failed to save organization details'
  } finally {
    savingDetails.value = false
  }
}
</script>

<template>
  <div>
    <PageHeader
      title="Settings"
      description="Organization details and SMS notification alerts."
    />

    <p v-if="org.error && !auth.isSuperAdmin" class="mb-4 text-sm text-rose-600">{{ org.error }}</p>
    <p v-else-if="platformError" class="mb-4 text-sm text-rose-600">{{ platformError }}</p>

    <div class="grid gap-6 lg:grid-cols-2">
      <AppCard
        v-if="auth.isSuperAdmin"
        title="Organization"
      >
        <dl class="space-y-4 text-sm">
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Name</dt>
            <dd class="font-medium text-slate-900">{{ displayOrgName }}</dd>
          </div>
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Organization ID</dt>
            <dd class="font-mono text-xs text-slate-600">
              {{ auth.user?.organizationId || '—' }}
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

      <AppCard
        v-else
        title="Organization"
        subtitle="Update the name, email, and phone used for login and SMS alerts."
      >
        <form class="space-y-4" @submit.prevent="saveOrgDetails">
          <FormField label="Name" required>
            <AppInput v-model="orgForm.name" type="text" maxlength="150" />
          </FormField>
          <FormField label="Email" required hint="Used for login when it matches the admin user email.">
            <AppInput v-model="orgForm.email" type="email" maxlength="180" />
          </FormField>
          <FormField
            label="Phone"
            required
            hint="Kenyan mobile used for login and low-balance SMS, for example 0711766223."
          >
            <AppInput v-model="orgForm.phone" type="tel" maxlength="30" />
          </FormField>
          <dl class="space-y-4 text-sm">
            <div
              v-if="org.currentOrganization?.accountType || auth.user?.accountType"
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
              v-if="org.currentOrganization?.mpesaAccountRef"
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
            <div v-if="auth.user?.expiresAt" class="flex justify-between gap-4 border-b border-slate-100 pb-3">
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
          <p v-if="detailsError" class="text-sm text-rose-600">{{ detailsError }}</p>
          <p v-if="detailsMessage" class="text-sm text-emerald-700">{{ detailsMessage }}</p>
          <AppButton type="submit" :loading="savingDetails">Save organization details</AppButton>
        </form>
      </AppCard>

      <AppCard
        v-if="auth.isSuperAdmin"
        title="SMS notifications"
        subtitle="Live values from SMS settings. Edit templates and the platform switch there."
      >
        <dl v-if="platformSettings" class="space-y-4 text-sm">
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Enabled</dt>
            <dd>
              <StatusBadge :variant="platformSettings.enabled ? 'success' : 'neutral'">
                {{ platformSettings.enabled ? 'On' : 'Off' }}
              </StatusBadge>
            </dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-slate-500">Low-balance threshold</dt>
            <dd class="font-medium text-slate-900">
              {{ formatCurrency(Number(platformSettings.lowBalanceThreshold)) }}
            </dd>
          </div>
        </dl>
        <p v-else class="text-sm text-slate-500">Loading notification settings…</p>
        <p v-if="platformError" class="mt-3 text-sm text-red-600">{{ platformError }}</p>
        <p class="mt-4 text-sm">
          <RouterLink to="/admin/sms-settings" class="font-medium text-brand-700 hover:text-brand-800">
            Open SMS settings
          </RouterLink>
        </p>
      </AppCard>

      <AppCard
        v-if="auth.isSuperAdmin"
        title="M-Pesa C2B URLs"
        subtitle="These two URLs must be registered on the platform Paybill in Daraja."
      >
        <dl v-if="c2bUrls" class="space-y-4 text-sm">
          <div class="flex justify-between gap-4 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Paybill</dt>
            <dd class="font-mono text-xs text-slate-700">{{ c2bUrls.shortcode || '—' }}</dd>
          </div>
          <div class="flex flex-col gap-1 border-b border-slate-100 pb-3">
            <dt class="text-slate-500">Confirmation URL</dt>
            <dd class="break-all font-mono text-xs text-slate-700">{{ c2bUrls.confirmationUrl }}</dd>
          </div>
          <div class="flex flex-col gap-1">
            <dt class="text-slate-500">Validation URL</dt>
            <dd class="break-all font-mono text-xs text-slate-700">{{ c2bUrls.validationUrl }}</dd>
          </div>
        </dl>
        <p v-else class="text-sm text-slate-500">Loading C2B URLs…</p>
        <p v-if="c2bMessage" class="mt-3 text-sm text-emerald-700">{{ c2bMessage }}</p>
        <p v-if="c2bError" class="mt-3 text-sm text-red-600">{{ c2bError }}</p>
        <div class="mt-4">
          <AppButton :loading="registeringC2b" @click="registerC2bUrls">Register C2B URLs</AppButton>
        </div>
      </AppCard>

      <AppCard
        v-else-if="!auth.isSuperAdmin"
        title="SMS notifications"
        subtitle="Welcome, top-up, and low-balance SMS to the organization phone. These messages are not billed to the wallet."
      >
        <form class="space-y-4" @submit.prevent="saveSettings">
          <label class="flex items-start gap-3 text-sm">
            <input
              v-model="notificationsEnabled"
              class="mt-1"
              type="checkbox"
            />
            <span>
              <span class="font-medium text-slate-800">Enable SMS notifications</span>
              <span class="mt-0.5 block text-xs text-slate-500">
                Turn off to stop welcome, top-up receipt, and low-balance alerts.
              </span>
            </span>
          </label>
          <p
            v-if="!platformNotificationsOn"
            class="rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-800"
          >
            Platform SMS notifications are currently off, so alerts will not send until they are enabled on the server.
          </p>
          <FormField
            label="Alert when balance reaches (KES)"
            :hint="`Message is sent to ${orgPhone}. Alert fires once each time the wallet crosses this amount from above.`"
            :error="localError"
            required
          >
            <AppInput
              v-model="threshold"
              type="number"
              min="0"
              step="1"
              :placeholder="thresholdPlaceholder"
            />
          </FormField>
          <p v-if="savedMessage" class="text-sm text-emerald-700">{{ savedMessage }}</p>
          <AppButton type="submit" :loading="saving">Save settings</AppButton>
        </form>
      </AppCard>
    </div>
  </div>
</template>
