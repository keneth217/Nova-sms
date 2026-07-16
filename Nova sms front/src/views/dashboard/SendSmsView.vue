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
import { estimateSmsCost, formatCurrency, smsPageCount } from '@/utils/format'

const sms = useSmsStore()
const wallet = useWalletStore()
const success = ref('')
const error = ref('')

const form = reactive({
  senderId: '',
  recipient: '',
  message: '',
})

const approvedSenders = computed(() => sms.senderIds.filter((s) => s.status === 'APPROVED'))

const pages = computed(() => smsPageCount(form.message))
const cost = computed(() => estimateSmsCost(form.message, form.recipient ? 1 : 0, wallet.smsCost))

onMounted(async () => {
  await Promise.all([sms.fetchSenderIds(), wallet.fetchBalance()])
  const first = approvedSenders.value[0]
  if (first) form.senderId = first.senderName
})

async function onSubmit() {
  success.value = ''
  error.value = ''
  try {
    const result = await sms.sendSms({
      senderId: form.senderId || undefined,
      recipient: form.recipient,
      message: form.message,
    })
    success.value = `Message queued to ${result.recipient} (${result.status}).`
    form.message = ''
    form.recipient = ''
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to send'
  }
}
</script>

<template>
  <div>
    <PageHeader
      title="Send SMS"
      description="Compose and send a single transactional or OTP message."
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
            :disabled="!form.message || !form.recipient"
          >
            Send SMS
          </AppButton>
        </form>
      </AppCard>

      <AppCard title="Tips">
        <ul class="space-y-3 text-sm text-slate-600">
          <li>Keep OTPs under 160 characters to use a single SMS page.</li>
          <li>Only approved sender IDs appear in the dropdown.</li>
          <li>Shared platform sender IDs are marked and available to all orgs.</li>
          <li>
            Current wallet balance:
            <strong>{{ formatCurrency(wallet.formattedBalance) }}</strong>
          </li>
        </ul>
      </AppCard>
    </div>
  </div>
</template>
