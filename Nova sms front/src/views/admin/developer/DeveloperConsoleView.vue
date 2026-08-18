<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { apiClientService } from '@/api/api-client.service'
import type { ApiClient } from '@/models/api-client.model'
import type { SmsMessage } from '@/models/sms.model'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import FormField from '@/components/common/FormField.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'

const clients = ref<ApiClient[]>([])
const sending = ref(false)
const checking = ref(false)
const error = ref('')
const result = ref<SmsMessage | null>(null)
const form = reactive({
  clientId: '',
  recipient: '',
  message: 'Hello from the Nova SMS test console.',
  senderId: '',
})

onMounted(async () => {
  try {
    const page = await apiClientService.listAll({ size: 100 })
    clients.value = page.content.filter((c) => c.status === 'ACTIVE')
    form.clientId = clients.value[0]?.id || ''
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load API clients'
  }
})

async function send() {
  error.value = ''
  result.value = null
  if (!form.clientId) {
    error.value = 'Select an API client.'
    return
  }
  sending.value = true
  try {
    result.value = await apiClientService.testSend(form.clientId, {
      recipient: form.recipient.trim(),
      message: form.message,
      senderId: form.senderId.trim() || undefined,
    })
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Send failed'
  } finally {
    sending.value = false
  }
}

async function refreshStatus() {
  if (!result.value?.id) return
  checking.value = true
  error.value = ''
  try {
    result.value = await apiClientService.refreshStatus(result.value.id)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Status refresh failed'
  } finally {
    checking.value = false
  }
}
</script>

<template>
  <div>
    <PageHeader
      title="API test console"
      description="Send a test SMS as the selected API client using your Super Admin session. The API key is never loaded into the browser."
    />

    <AppCard title="Send SMS">
      <form class="space-y-4" @submit.prevent="send">
        <FormField label="API client" required>
          <AppSelect v-model="form.clientId">
            <option value="" disabled>Select an API client</option>
            <option v-for="client in clients" :key="client.id" :value="client.id">
              {{ client.name }} · {{ client.organizationName || 'Organization' }} ({{ client.apiKeyPrefix }}…)
            </option>
          </AppSelect>
        </FormField>
        <FormField label="Recipient" required hint="07…, 01…, 254…, or +254…">
          <AppInput v-model="form.recipient" placeholder="254712345678" />
        </FormField>
        <FormField label="Sender ID" hint="Optional. Must already be approved for the organization.">
          <AppInput v-model="form.senderId" placeholder="Leave blank for TALK-SASA" />
        </FormField>
        <FormField label="Message" required>
          <AppInput v-model="form.message" type="textarea" :rows="4" />
        </FormField>
        <p v-if="error" class="text-sm text-rose-600">{{ error }}</p>
        <AppButton type="submit" :loading="sending">Send test SMS</AppButton>
      </form>
    </AppCard>

    <AppCard v-if="result" class="mt-6" title="Result">
      <dl class="grid gap-3 text-sm sm:grid-cols-2">
        <div>
          <dt class="text-slate-500">Nova SMS id</dt>
          <dd class="font-mono text-slate-900">{{ result.id }}</dd>
        </div>
        <div>
          <dt class="text-slate-500">Status</dt>
          <dd><EntityStatusBadge :status="result.status" /></dd>
        </div>
        <div>
          <dt class="text-slate-500">Recipient</dt>
          <dd class="font-mono">{{ result.recipient }}</dd>
        </div>
        <div>
          <dt class="text-slate-500">SMS units</dt>
          <dd>{{ result.smsUnits ?? '—' }}</dd>
        </div>
        <div class="sm:col-span-2">
          <dt class="text-slate-500">Failure reason</dt>
          <dd>{{ result.failureReason || '—' }}</dd>
        </div>
      </dl>
      <AppButton class="mt-4" variant="secondary" :loading="checking" @click="refreshStatus">
        Check status
      </AppButton>
    </AppCard>
  </div>
</template>
