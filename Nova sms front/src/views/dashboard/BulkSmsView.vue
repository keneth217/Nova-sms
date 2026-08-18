<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useSmsStore } from '@/stores/sms.store'
import { useWalletStore } from '@/stores/wallet.store'
import { contactService } from '@/api/contact.service'
import type { Contact, ContactGroup } from '@/models/contact.model'
import type { MessageChannel } from '@/models/sms.model'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import FormField from '@/components/common/FormField.vue'
import ContactPicker from '@/components/common/ContactPicker.vue'
import {
  estimateSmsCost,
  estimateWhatsAppCost,
  formatCurrency,
  formatDate,
  parsePhoneList,
  analyzeSms,
  summarizeBulkSmsResult,
} from '@/utils/format'

type RecipientSource = 'contacts' | 'group' | 'manual'

const route = useRoute()
const sms = useSmsStore()
const wallet = useWalletStore()
const channel = computed<MessageChannel>(() =>
  route.meta.channel === 'WHATSAPP' ? 'WHATSAPP' : 'SMS',
)
const isWhatsApp = computed(() => channel.value === 'WHATSAPP')
const channelLabel = computed(() => (isWhatsApp.value ? 'WhatsApp' : 'SMS'))
const groups = ref<ContactGroup[]>([])
const contacts = ref<Contact[]>([])
const selectedContactIds = ref<string[]>([])
const recipientSource = ref<RecipientSource>('contacts')
const success = ref('')
const error = ref('')
const csvHint = ref('')

const form = reactive({
  pasteNumbers: '',
  groupId: '',
  message: '',
  sendLater: false,
  scheduledAt: '',
})

const pastedRecipients = computed(() => parsePhoneList(form.pasteNumbers))

const selectedContactPhones = computed(() => {
  const selected = new Set(selectedContactIds.value)
  return contacts.value.filter((c) => selected.has(c.id)).map((c) => c.phone)
})

const recipientCount = computed(() => {
  if (recipientSource.value === 'group') {
    return groups.value.find((g) => g.id === form.groupId)?.contactCount ?? 0
  }
  if (recipientSource.value === 'contacts') {
    return selectedContactPhones.value.length
  }
  return pastedRecipients.value.length
})

const pages = computed(() => (isWhatsApp.value ? (form.message ? 1 : 0) : analyzeSms(form.message).units))
const encoding = computed(() => analyzeSms(form.message).encoding)
const cost = computed(() =>
  isWhatsApp.value
    ? estimateWhatsAppCost(form.message, recipientCount.value, wallet.smsCost)
    : estimateSmsCost(form.message, recipientCount.value, wallet.smsCost),
)
const remainingAfter = computed(() => wallet.formattedBalance - cost.value)

const minScheduleLocal = computed(() => {
  const d = new Date(Date.now() + 60_000)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
})

watch(recipientSource, () => {
  form.groupId = ''
  form.pasteNumbers = ''
  selectedContactIds.value = []
  csvHint.value = ''
})

onMounted(async () => {
  await wallet.fetchBalance()
  const [groupList, contactPage] = await Promise.all([
    contactService.listGroups(),
    contactService.listContacts({ size: 500 }),
  ])
  groups.value = groupList
  contacts.value = contactPage.content
})

function onCsvUpload(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    const text = String(reader.result || '')
    const phones = text
      .split(/\r?\n/)
      .flatMap((line) => line.split(','))
      .map((p) => p.trim().replace(/"/g, ''))
      .filter((p) => /^\d{9,15}$/.test(p))
    form.pasteNumbers = phones.join('\n')
    csvHint.value = `Imported ${phones.length} numbers from ${file.name}`
  }
  reader.readAsText(file)
}

async function onSubmit() {
  success.value = ''
  error.value = ''
  try {
    if (recipientCount.value === 0) {
      error.value =
        recipientSource.value === 'group'
          ? 'Select a contact group.'
          : recipientSource.value === 'contacts'
            ? 'Select at least one contact.'
            : 'Paste or upload at least one phone number.'
      return
    }

    const payload = {
      message: form.message,
      recipients:
        recipientSource.value === 'group'
          ? undefined
          : recipientSource.value === 'contacts'
            ? selectedContactPhones.value
            : pastedRecipients.value,
      groupId: recipientSource.value === 'group' ? form.groupId || undefined : undefined,
    }

    if (form.sendLater) {
      if (!form.scheduledAt) {
        error.value = 'Choose a date and time for the reminder.'
        return
      }
      const when = new Date(form.scheduledAt)
      if (Number.isNaN(when.getTime()) || when.getTime() <= Date.now()) {
        error.value = 'Reminder time must be in the future.'
        return
      }
      const result = await sms.scheduleSms(
        {
          ...payload,
          scheduledAt: when.toISOString(),
        },
        channel.value,
      )
      success.value = `Reminder scheduled for ${formatDate(when.toISOString())}: ${result.queuedCount} messages (batch ${result.batchId}).`
    } else {
      const result = await sms.sendBulk(payload, channel.value)
      const summary = summarizeBulkSmsResult(result)
      if (summary.ok) success.value = summary.text
      else error.value = summary.text
      await wallet.fetchBalance()
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Campaign failed'
  }
}
</script>

<template>
  <div>
    <PageHeader
      :title="`Bulk ${channelLabel}`"
      :description="
        isWhatsApp
          ? 'Send WhatsApp to selected contacts, a group, or pasted / CSV numbers.'
          : 'Send to selected contacts, a group, or pasted / CSV numbers.'
      "
    />

    <div class="grid gap-6 lg:grid-cols-3">
      <AppCard class="lg:col-span-2" title="Campaign composer">
        <form class="space-y-4" @submit.prevent="onSubmit">
          <div>
            <p class="mb-2 text-sm font-medium text-slate-700">Recipients</p>
            <div class="mb-3 grid grid-cols-3 gap-2 rounded-lg bg-slate-100 p-1">
              <button
                v-for="option in [
                  { id: 'contacts', label: 'Contacts' },
                  { id: 'group', label: 'Group' },
                  { id: 'manual', label: 'Paste / CSV' },
                ] as const"
                :key="option.id"
                type="button"
                class="rounded-md px-2 py-2 text-xs font-semibold transition sm:text-sm"
                :class="
                  recipientSource === option.id
                    ? 'bg-white text-slate-900 shadow-sm'
                    : 'text-slate-500 hover:text-slate-700'
                "
                @click="recipientSource = option.id"
              >
                {{ option.label }}
              </button>
            </div>

            <FormField v-if="recipientSource === 'contacts'" label="Select contacts" required>
              <ContactPicker v-model="selectedContactIds" :contacts="contacts" multiple />
            </FormField>

            <FormField v-else-if="recipientSource === 'group'" label="Contact group" required>
              <AppSelect v-model="form.groupId" placeholder="Select a group">
                <option v-for="g in groups" :key="g.id" :value="g.id">
                  {{ g.name }} ({{ g.contactCount }})
                </option>
              </AppSelect>
            </FormField>

            <div v-else class="space-y-4">
              <FormField label="Upload CSV" hint="One phone number per row or comma-separated">
                <input
                  type="file"
                  accept=".csv,text/csv"
                  class="block w-full text-sm text-slate-600 file:mr-3 file:rounded-lg file:border-0 file:bg-brand-50 file:px-3 file:py-2 file:text-sm file:font-medium file:text-brand-700"
                  @change="onCsvUpload"
                />
                <p v-if="csvHint" class="mt-1 text-xs text-brand-700">{{ csvHint }}</p>
              </FormField>
              <FormField label="Paste phone numbers" hint="One per line, or comma-separated">
                <AppInput
                  v-model="form.pasteNumbers"
                  type="textarea"
                  :rows="5"
                  placeholder="0712345678&#10;0722334455"
                />
              </FormField>
            </div>
          </div>

          <FormField label="Message" required>
            <AppInput v-model="form.message" type="textarea" :rows="5" />
          </FormField>

          <div class="rounded-xl border border-slate-200 bg-slate-50/80 p-4">
            <label class="flex cursor-pointer items-start gap-3">
              <input
                v-model="form.sendLater"
                type="checkbox"
                class="mt-1 h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
              />
              <span>
                <span class="block text-sm font-semibold text-slate-900">Schedule reminder</span>
                <span class="mt-0.5 block text-xs text-slate-500">
                  Queue this campaign to send automatically later.
                </span>
              </span>
            </label>
            <FormField
              v-if="form.sendLater"
              class="mt-4"
              label="Send at"
              required
              hint="Local time — must be in the future"
            >
              <AppInput
                v-model="form.scheduledAt"
                type="datetime-local"
                :min="minScheduleLocal"
              />
            </FormField>
          </div>

          <p v-if="success" class="text-sm text-emerald-700">{{ success }}</p>
          <p v-if="error" class="text-sm text-rose-600">{{ error }}</p>

          <AppButton
            type="submit"
            :loading="sms.loading"
            :disabled="!form.message || recipientCount === 0 || (form.sendLater && !form.scheduledAt)"
          >
            {{ form.sendLater ? 'Schedule reminder' : `Send ${channelLabel} campaign` }}
          </AppButton>
        </form>
      </AppCard>

      <AppCard title="Estimate">
        <dl class="space-y-4 text-sm">
          <div class="flex justify-between">
            <dt class="text-slate-500">Recipients</dt>
            <dd class="font-semibold text-slate-900">{{ recipientCount }}</dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-slate-500">Characters</dt>
            <dd class="font-semibold text-slate-900">{{ form.message.length }}</dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-slate-500">{{ isWhatsApp ? 'WhatsApp units' : 'SMS units' }}</dt>
            <dd class="font-semibold text-slate-900">
              {{ pages }}
              <span v-if="!isWhatsApp" class="text-xs font-normal text-slate-400">({{ encoding }})</span>
              <span v-else class="text-xs font-normal text-slate-400">(1 per recipient)</span>
            </dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-slate-500">Unit cost</dt>
            <dd class="font-semibold text-slate-900">{{ formatCurrency(wallet.smsCost) }}</dd>
          </div>
          <div class="border-t border-slate-100 pt-4 flex justify-between">
            <dt class="text-slate-500">Estimated total</dt>
            <dd class="text-lg font-semibold text-brand-700">{{ formatCurrency(cost) }}</dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-slate-500">Balance after sending</dt>
            <dd class="font-semibold text-slate-900">{{ formatCurrency(remainingAfter) }}</dd>
          </div>
          <p v-if="form.sendLater && form.scheduledAt" class="text-xs text-slate-500">
            Will send {{ formatDate(new Date(form.scheduledAt).toISOString()) }}
          </p>
        </dl>
      </AppCard>
    </div>
  </div>
</template>
