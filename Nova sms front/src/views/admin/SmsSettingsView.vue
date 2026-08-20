<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { organizationService } from '@/api/organization.service'
import type { PlatformNotificationSettings, TalkSasaAccount } from '@/models/organization.model'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import FormField from '@/components/common/FormField.vue'
import { formatNumber } from '@/utils/format'

const loading = ref(true)
const saving = ref(false)
const error = ref('')
const message = ref('')
const talksasa = ref<TalkSasaAccount | null>(null)
const talksasaError = ref('')

const form = reactive({
  enabled: true,
  lowBalanceThreshold: '50.00',
  portalUrl: '',
  welcomeTemplate: '',
  topupTemplate: '',
  collectionTemplate: '',
  lowBalanceTemplate: '',
  platformTopupTemplate: '',
  providerLowTemplate: '',
  providerExposureTemplate: '',
  collectionAccounts: 'SHEILA\nKENETH',
  collectionNotifyPhones: '',
})

const talksasaRemaining = computed(() => talksasa.value?.balance?.remainingUnits)
const talksasaBelowThreshold = computed(() => {
  const remaining = talksasaRemaining.value
  const threshold = Number(form.lowBalanceThreshold)
  if (remaining == null || !Number.isFinite(threshold)) return false
  return Number(remaining) <= threshold
})

function linesToList(value: string): string[] {
  return value
    .split(/[\n,]+/)
    .map((part) => part.trim())
    .filter(Boolean)
}

function applySettings(data: PlatformNotificationSettings) {
  form.enabled = data.enabled
  form.lowBalanceThreshold = Number(data.lowBalanceThreshold).toFixed(2)
  form.portalUrl = data.portalUrl || ''
  form.welcomeTemplate = data.welcomeTemplate || ''
  form.topupTemplate = data.topupTemplate || ''
  form.collectionTemplate = data.collectionTemplate || ''
  form.lowBalanceTemplate = data.lowBalanceTemplate || ''
  form.platformTopupTemplate = data.platformTopupTemplate || ''
  form.providerLowTemplate = data.providerLowTemplate || ''
  form.providerExposureTemplate = data.providerExposureTemplate || ''
  form.collectionAccounts = (data.collectionAccounts || []).join('\n')
  form.collectionNotifyPhones = (data.collectionNotifyPhones || []).join('\n')
}

onMounted(async () => {
  loading.value = true
  error.value = ''
  talksasaError.value = ''
  try {
    const [settings] = await Promise.all([
      organizationService.getNotificationSettings(),
      organizationService
        .getTalkSasaAccount()
        .then((account) => {
          talksasa.value = account
        })
        .catch((e) => {
          talksasaError.value = e instanceof Error ? e.message : 'Failed to load TalkSasa remaining units'
        }),
    ])
    applySettings(settings)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load SMS settings'
  } finally {
    loading.value = false
  }
})

async function save() {
  message.value = ''
  error.value = ''
  const threshold = Number(form.lowBalanceThreshold)
  if (!Number.isFinite(threshold) || threshold < 0) {
    error.value = 'Enter a low-balance threshold of 0 or more.'
    return
  }
  if (!form.welcomeTemplate.trim() || !form.topupTemplate.trim()
      || !form.collectionTemplate.trim() || !form.lowBalanceTemplate.trim()
      || !form.platformTopupTemplate.trim() || !form.providerLowTemplate.trim()
      || !form.providerExposureTemplate.trim()) {
    error.value = 'All message templates are required.'
    return
  }
  if (form.platformTopupTemplate.trim().length > 160) {
    error.value = 'Platform-owner top-up SMS must be 160 characters or fewer.'
    return
  }
  if (form.providerLowTemplate.trim().length > 160) {
    error.value = 'TalkSasa low-units SMS must be 160 characters or fewer.'
    return
  }
  if (form.providerExposureTemplate.trim().length > 160) {
    error.value = 'Wallet-exposure SMS must be 160 characters or fewer.'
    return
  }
  const accounts = linesToList(form.collectionAccounts)
  const phones = linesToList(form.collectionNotifyPhones)
  if (accounts.length === 0) {
    error.value = 'Add at least one Paybill collection account (for example SHEILA).'
    return
  }
  saving.value = true
  try {
    applySettings(await organizationService.updateNotificationSettings({
      enabled: form.enabled,
      lowBalanceThreshold: threshold,
      portalUrl: form.portalUrl.trim(),
      welcomeTemplate: form.welcomeTemplate.trim(),
      topupTemplate: form.topupTemplate.trim(),
      collectionTemplate: form.collectionTemplate.trim(),
      lowBalanceTemplate: form.lowBalanceTemplate.trim(),
      platformTopupTemplate: form.platformTopupTemplate.trim(),
      providerLowTemplate: form.providerLowTemplate.trim(),
      providerExposureTemplate: form.providerExposureTemplate.trim(),
      collectionAccounts: accounts,
      collectionNotifyPhones: phones,
    }))
    message.value = 'SMS settings saved. New messages use this copy immediately.'
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to save SMS settings'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <PageHeader
      title="SMS settings"
      description="Platform notifications, Super Admin default threshold, Paybill collection accounts, and SMS copy. Organizations can still turn their own alerts off and set their own threshold."
    />

    <p v-if="error" class="mb-4 text-sm text-rose-600">{{ error }}</p>
    <p v-else-if="loading" class="mb-4 text-sm text-slate-500">Loading SMS settings…</p>

    <form v-else class="space-y-6" @submit.prevent="save">
      <AppCard title="Defaults" subtitle="New organizations inherit this threshold. Each organization can override it in Settings.">
        <div class="space-y-4">
          <label class="flex items-start gap-3 text-sm">
            <input v-model="form.enabled" class="mt-1" type="checkbox" />
            <span>
              <span class="font-medium text-slate-800">Enable platform SMS notifications</span>
              <span class="mt-0.5 block text-xs text-slate-500">
                Off stops welcome, org top-up, low-balance, collection, platform-owner top-up, TalkSasa low-units, and wallet-exposure SMS.
              </span>
            </span>
          </label>
          <div class="grid gap-4 sm:grid-cols-2">
            <FormField
              label="Super Admin low-balance threshold"
              hint="Organizations inherit this as their wallet alert in KES. Platform-owner phones are also SMS'd when TalkSasa remaining units fall to this number or below."
              required
            >
              <AppInput v-model="form.lowBalanceThreshold" type="number" min="0" step="0.01" />
            </FormField>
            <FormField label="Portal URL" hint="Used by {portalUrl} in the welcome template.">
              <AppInput v-model="form.portalUrl" type="url" />
            </FormField>
          </div>
          <p v-if="talksasaError" class="text-sm text-rose-600">{{ talksasaError }}</p>
          <p
            v-else-if="talksasaRemaining == null"
            class="text-sm text-slate-500"
          >
            TalkSasa remaining units could not be loaded yet.
          </p>
          <p
            v-else
            class="text-sm"
            :class="talksasaBelowThreshold ? 'text-rose-700' : 'text-slate-600'"
          >
            TalkSasa remaining
            <span class="font-medium">{{ formatNumber(Number(talksasaRemaining)) }}</span>
            units versus this threshold of
            <span class="font-medium">{{ form.lowBalanceThreshold }}</span>.
            <span v-if="talksasaBelowThreshold" class="font-medium">
              At or below threshold — platform owner is alerted once until units recover.
            </span>
            <span v-else>Above threshold — no owner alert.</span>
          </p>
        </div>
      </AppCard>

      <AppCard
        title="Paybill collection accounts"
        subtitle="These BillRef names never credit an organization wallet. Matching is case-insensitive."
      >
        <div class="grid gap-4 sm:grid-cols-2">
          <FormField
            label="Collection accounts"
            hint="One name per line, for example SHEILA and KENETH. Paybill payments to these accounts are recorded as collections."
            required
          >
            <AppInput v-model="form.collectionAccounts" type="textarea" :rows="4" />
          </FormField>
          <FormField
            label="Notify phones"
            hint="Kenyan mobiles that receive collection SMS, organization top-up alerts, TalkSasa low-units alerts, and wallet-exposure alerts. One per line. Not billed to any organization wallet."
          >
            <AppInput v-model="form.collectionNotifyPhones" type="textarea" :rows="4" />
          </FormField>
        </div>
      </AppCard>

      <AppCard
        title="Message templates"
        subtitle="Keep placeholders in curly braces. Safaricom SMS is 160 characters per segment."
      >
        <div class="space-y-4">
          <FormField label="Welcome" hint="Placeholders: {name} {portalUrl}">
            <AppInput v-model="form.welcomeTemplate" type="textarea" :rows="3" />
          </FormField>
          <FormField label="Wallet top-up" hint='Placeholders: {amount} {receipt} {balance} — {receipt} is empty or " Receipt UHJA541HGH."'>
            <AppInput v-model="form.topupTemplate" type="textarea" :rows="3" />
          </FormField>
          <FormField label="Paybill collection" hint="Placeholders: {amount} {payer} {account} {receipt} — {account} is Sheila or Keneth">
            <AppInput v-model="form.collectionTemplate" type="textarea" :rows="3" />
          </FormField>
          <FormField label="Low balance" hint="Placeholders: {balance}">
            <AppInput v-model="form.lowBalanceTemplate" type="textarea" :rows="3" />
          </FormField>
          <FormField
            label="Platform owner — org wallet top-up"
            hint='One SMS segment (160 chars). Placeholders: {name} {account} {amount} {receipt} {balance} {time}. {receipt} is empty or " Ref QWE123XYZ.". {time} is Nairobi dd/MM HH:mm.'
          >
            <AppInput v-model="form.platformTopupTemplate" type="textarea" :rows="3" maxlength="160" />
          </FormField>
          <FormField
            label="Platform owner — TalkSasa low units"
            hint="One SMS segment (160 chars). Placeholders: {units} {threshold}. Sent once when TalkSasa remaining units fall to the Super Admin threshold or below."
          >
            <AppInput v-model="form.providerLowTemplate" type="textarea" :rows="3" maxlength="160" />
          </FormField>
          <FormField
            label="Platform owner — org wallets exceed TalkSasa"
            hint="One SMS segment (160 chars). Placeholders: {wallets} {units}. Sent once when the sum of organization wallets is greater than TalkSasa remaining units."
          >
            <AppInput v-model="form.providerExposureTemplate" type="textarea" :rows="3" maxlength="160" />
          </FormField>
        </div>
      </AppCard>

      <div class="flex flex-wrap items-center gap-3">
        <AppButton type="submit" :loading="saving">Save SMS settings</AppButton>
        <p v-if="message" class="text-sm text-brand-700">{{ message }}</p>
      </div>
    </form>
  </div>
</template>
