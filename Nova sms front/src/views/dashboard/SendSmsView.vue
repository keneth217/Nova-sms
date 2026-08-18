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
  analyzeSms,
  summarizeBulkSmsResult,
  summarizeSingleSmsResult,
} from '@/utils/format'

type RecipientSource = 'phone' | 'contact' | 'group'

const route = useRoute()
const sms = useSmsStore()
const wallet = useWalletStore()
const channel = computed<MessageChannel>(() =>
  route.meta.channel === 'WHATSAPP' ? 'WHATSAPP' : 'SMS',
)
const isWhatsApp = computed(() => channel.value === 'WHATSAPP')
const channelLabel = computed(() => (isWhatsApp.value ? 'WhatsApp' : 'SMS'))
const success = ref('')
const error = ref('')
const contacts = ref<Contact[]>([])
const groups = ref<ContactGroup[]>([])
const selectedContactIds = ref<string[]>([])
const recipientSource = ref<RecipientSource>('phone')

const form = reactive({
  recipient: '',
  groupId: '',
  message: '',
  sendLater: false,
  scheduledAt: '',
})

const selectedContact = computed(() =>
  contacts.value.find((c) => c.id === selectedContactIds.value[0]) || null,
)

const selectedGroup = computed(() => groups.value.find((g) => g.id === form.groupId) || null)

const resolvedRecipients = computed(() => {
  if (recipientSource.value === 'contact' && selectedContact.value) {
    return [selectedContact.value.phone]
  }
  if (recipientSource.value === 'phone' && form.recipient.trim()) {
    return [form.recipient.trim()]
  }
  return [] as string[]
})

const recipientCount = computed(() => {
  if (recipientSource.value === 'group') {
    return selectedGroup.value?.contactCount ?? 0
  }
  return resolvedRecipients.value.length
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

const canSubmit = computed(() => {
  if (!form.message) return false
  if (form.sendLater && !form.scheduledAt) return false
  return recipientCount.value > 0
})

watch(recipientSource, () => {
  form.recipient = ''
  form.groupId = ''
  selectedContactIds.value = []
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

async function onSubmit() {
  success.value = ''
  error.value = ''
  try {
    if (recipientSource.value === 'group') {
      if (!form.groupId) {
        error.value = 'Select a contact group.'
        return
      }
    } else if (resolvedRecipients.value.length === 0) {
      error.value =
        recipientSource.value === 'contact'
          ? 'Select a contact.'
          : 'Enter a phone number.'
      return
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
          recipients: recipientSource.value === 'group' ? undefined : resolvedRecipients.value,
          groupId: recipientSource.value === 'group' ? form.groupId : undefined,
          message: form.message,
          scheduledAt: when.toISOString(),
        },
        channel.value,
      )
      success.value = `Reminder scheduled for ${formatDate(when.toISOString())} (${result.queuedCount} message${result.queuedCount === 1 ? '' : 's'}).`
    } else if (recipientSource.value === 'group' || resolvedRecipients.value.length > 1) {
      const result = await sms.sendBulk(
        {
          recipients: recipientSource.value === 'group' ? undefined : resolvedRecipients.value,
          groupId: recipientSource.value === 'group' ? form.groupId : undefined,
          message: form.message,
        },
        channel.value,
      )
      const summary = summarizeBulkSmsResult(result)
      if (summary.ok) success.value = summary.text
      else error.value = summary.text
    } else {
      const result = await sms.sendSms(
        {
          recipient: resolvedRecipients.value[0]!,
          message: form.message,
        },
        channel.value,
      )
      const summary = summarizeSingleSmsResult(result)
      if (summary.ok) success.value = summary.text
      else error.value = summary.text
    }

    if (!error.value) {
      form.message = ''
      form.recipient = ''
      form.groupId = ''
      form.scheduledAt = ''
      form.sendLater = false
      selectedContactIds.value = []
      await wallet.fetchBalance()
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to send'
  }
}
</script>

<template>
  <div>
    <PageHeader
      :title="`Send ${channelLabel}`"
      :description="
        isWhatsApp
          ? 'Send a WhatsApp message to a phone number, a saved contact, or a whole contact group.'
          : 'Send to a phone number, a saved contact, or a whole contact group.'
      "
    />

    <div class="mx-auto grid max-w-4xl gap-6 lg:grid-cols-3">
      <AppCard class="lg:col-span-2" title="Compose message">
        <form class="space-y-4" @submit.prevent="onSubmit">
          <div>
            <p class="mb-2 text-sm font-medium text-slate-700">Recipient</p>
            <div class="mb-3 grid grid-cols-3 gap-2 rounded-lg bg-slate-100 p-1">
              <button
                v-for="option in [
                  { id: 'phone', label: 'Phone' },
                  { id: 'contact', label: 'Contact' },
                  { id: 'group', label: 'Group' },
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

            <FormField v-if="recipientSource === 'phone'" label="Phone number" required hint="07… or 254… — formatted automatically">
              <AppInput v-model="form.recipient" type="tel" placeholder="0712345678" />
            </FormField>

            <FormField v-else-if="recipientSource === 'contact'" label="Select contact" required>
              <ContactPicker
                v-model="selectedContactIds"
                :contacts="contacts"
                :multiple="false"
                max-height-class="max-h-64"
              />
            </FormField>

            <FormField v-else label="Contact group" required>
              <AppSelect v-model="form.groupId" placeholder="Select a group">
                <option v-for="g in groups" :key="g.id" :value="g.id">
                  {{ g.name }} ({{ g.contactCount }})
                </option>
              </AppSelect>
            </FormField>
          </div>

          <FormField label="Message" required>
            <AppInput
              v-model="form.message"
              type="textarea"
              :rows="6"
              placeholder="Type your message…"
            />
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
                  Send this {{ channelLabel }} automatically on a later date and time.
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

          <div
            class="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-slate-50 px-4 py-3 text-sm"
          >
            <span class="text-slate-600">
              {{ recipientCount }} recipient{{ recipientCount === 1 ? '' : 's' }} ·
              {{ form.message.length }} characters
              <template v-if="!isWhatsApp">
                · {{ pages }} SMS unit{{ pages === 1 ? '' : 's' }}
                <span class="text-slate-400">({{ encoding }})</span>
              </template>
              <template v-else> · 1 WhatsApp unit each</template>
            </span>
            <span class="font-semibold text-slate-900"> Est. cost {{ formatCurrency(cost) }} </span>
          </div>
          <p class="text-xs text-slate-500">
            Balance {{ formatCurrency(wallet.formattedBalance) }}
            <span v-if="recipientCount > 0 && form.message">
              · After sending {{ formatCurrency(remainingAfter) }}
            </span>
          </p>

          <p v-if="success" class="text-sm text-emerald-700">{{ success }}</p>
          <p v-if="error" class="text-sm text-rose-600">{{ error }}</p>

          <AppButton type="submit" :loading="sms.loading" :disabled="!canSubmit">
            {{ form.sendLater ? 'Schedule reminder' : `Send ${channelLabel}` }}
          </AppButton>
        </form>
      </AppCard>

      <AppCard title="Tips">
        <ul class="space-y-3 text-sm text-slate-600">
          <li>If you omit a sender ID, Nova uses the TalkSasa default (currently TALK-SASA). Approved organization sender IDs are used when you select one.</li>
          <li>Pick a saved contact or send to an entire group in one go.</li>
          <li v-if="!isWhatsApp">Keep OTPs under 160 characters to use a single SMS page.</li>
          <li v-else>WhatsApp is billed as one unit per recipient. Recipients must be able to receive WhatsApp.</li>
          <li>Schedule reminders for appointments, events, or payment follow-ups.</li>
          <li>
            Current wallet balance:
            <strong>{{ formatCurrency(wallet.formattedBalance) }}</strong>
            · {{ formatCurrency(wallet.smsCost) }} per {{ isWhatsApp ? 'WhatsApp' : 'SMS' }} unit
          </li>
        </ul>
      </AppCard>
    </div>
  </div>
</template>
