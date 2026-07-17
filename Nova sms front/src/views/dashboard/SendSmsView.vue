<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useSmsStore } from '@/stores/sms.store'
import { useWalletStore } from '@/stores/wallet.store'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import FormField from '@/components/common/FormField.vue'
import { estimateSmsCost, formatCurrency, formatDate, smsPageCount } from '@/utils/format'

const sms = useSmsStore()
const wallet = useWalletStore()
const success = ref('')
const error = ref('')

const form = reactive({
  senderId: '',
  recipient: '',
  message: '',
  sendLater: false,
  scheduledAt: '',
})

const approvedSenders = computed(() => sms.senderIds.filter((s) => s.status === 'APPROVED'))

const pages = computed(() => smsPageCount(form.message))
const cost = computed(() => estimateSmsCost(form.message, form.recipient ? 1 : 0, wallet.smsCost))

const minScheduleLocal = computed(() => {
  const d = new Date(Date.now() + 60_000)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
})

onMounted(async () => {
  await Promise.all([sms.fetchSenderIds(), wallet.fetchBalance()])
  const first = approvedSenders.value[0]
  if (first) form.senderId = first.senderName
})

async function onSubmit() {
  success.value = ''
  error.value = ''
  try {
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
      const result = await sms.scheduleSms({
        senderId: form.senderId || undefined,
        recipients: [form.recipient],
        message: form.message,
        scheduledAt: when.toISOString(),
      })
      success.value = `Reminder scheduled for ${formatDate(when.toISOString())} (${result.queuedCount} message).`
    } else {
      const result = await sms.sendSms({
        senderId: form.senderId || undefined,
        recipient: form.recipient,
        message: form.message,
      })
      success.value = `Message queued to ${result.recipient} (${result.status}).`
    }
    form.message = ''
    form.recipient = ''
    form.scheduledAt = ''
    form.sendLater = false
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to send'
  }
}
</script>

<template>
  <div>
    <PageHeader
      title="Send SMS"
      description="Send now, or set a reminder to deliver automatically later."
    />

    <div class="mx-auto grid max-w-4xl gap-6 lg:grid-cols-3">
      <AppCard class="lg:col-span-2" title="Compose message">
        <form class="space-y-4" @submit.prevent="onSubmit">
          <FormField label="Sender ID" required>
            <AppSelect v-model="form.senderId" placeholder="Select sender ID">
              <option v-for="s in approvedSenders" :key="s.id" :value="s.senderName">
                {{ s.senderName }}{{ s.platformDefault ? ' (shared)' : '' }}
              </option>
            </AppSelect>
          </FormField>
          <FormField label="Phone number" required hint="07… or 254… — formatted automatically">
            <AppInput v-model="form.recipient" type="tel" placeholder="0712345678" />
          </FormField>
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
                  Send this SMS automatically on a later date and time.
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
              {{ form.message.length }} characters · {{ pages }} page{{ pages === 1 ? '' : 's' }}
            </span>
            <span class="font-semibold text-slate-900"> Est. cost {{ formatCurrency(cost) }} </span>
          </div>

          <p v-if="success" class="text-sm text-emerald-700">{{ success }}</p>
          <p v-if="error" class="text-sm text-rose-600">{{ error }}</p>

          <AppButton
            type="submit"
            :loading="sms.loading"
            :disabled="!form.message || !form.recipient || (form.sendLater && !form.scheduledAt)"
          >
            {{ form.sendLater ? 'Schedule reminder' : 'Send SMS' }}
          </AppButton>
        </form>
      </AppCard>

      <AppCard title="Tips">
        <ul class="space-y-3 text-sm text-slate-600">
          <li>Keep OTPs under 160 characters to use a single SMS page.</li>
          <li>Schedule reminders for appointments, events, or payment follow-ups.</li>
          <li>Wallet is charged when the reminder is scheduled, not when it sends.</li>
          <li>
            Current wallet balance:
            <strong>{{ formatCurrency(wallet.formattedBalance) }}</strong>
          </li>
        </ul>
      </AppCard>
    </div>
  </div>
</template>
