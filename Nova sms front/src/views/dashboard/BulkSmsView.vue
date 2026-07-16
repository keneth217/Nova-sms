<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useSmsStore } from '@/stores/sms.store'
import { useWalletStore } from '@/stores/wallet.store'
import { contactService } from '@/api/contact.service'
import type { ContactGroup } from '@/models/contact.model'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import FormField from '@/components/common/FormField.vue'
import { estimateSmsCost, formatCurrency, parsePhoneList, smsPageCount } from '@/utils/format'

const sms = useSmsStore()
const wallet = useWalletStore()
const groups = ref<ContactGroup[]>([])
const success = ref('')
const error = ref('')
const csvHint = ref('')

const form = reactive({
  senderId: '',
  pasteNumbers: '',
  groupId: '',
  message: '',
})

const recipients = computed(() => parsePhoneList(form.pasteNumbers))
const recipientCount = computed(() => {
  if (form.groupId) {
    return groups.value.find((g) => g.id === form.groupId)?.contactCount ?? 0
  }
  return recipients.value.length
})
const pages = computed(() => smsPageCount(form.message))
const cost = computed(() => estimateSmsCost(form.message, recipientCount.value, wallet.smsCost))
const approvedSenders = computed(() => sms.senderIds.filter((s) => s.status === 'APPROVED'))

onMounted(async () => {
  await Promise.all([sms.fetchSenderIds(), wallet.fetchBalance()])
  groups.value = await contactService.listGroups()
  const first = approvedSenders.value[0]
  if (first) form.senderId = first.senderName
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
    const result = await sms.sendBulk({
      senderId: form.senderId || undefined,
      message: form.message,
      recipients: form.groupId ? undefined : recipients.value,
      groupId: form.groupId || undefined,
    })
    success.value = `Campaign queued: ${result.queuedCount} messages (batch ${result.batchId}).`
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Campaign failed'
  }
}
</script>

<template>
  <div>
    <PageHeader
      title="Bulk SMS"
      description="Launch campaigns from CSV, pasted numbers, or contact groups."
    />

    <div class="grid gap-6 lg:grid-cols-3">
      <AppCard class="lg:col-span-2" title="Campaign composer">
        <form class="space-y-4" @submit.prevent="onSubmit">
          <FormField label="Sender ID" required>
            <AppSelect v-model="form.senderId">
              <option v-for="s in approvedSenders" :key="s.id" :value="s.senderName">
                {{ s.senderName }}
              </option>
            </AppSelect>
          </FormField>

          <div class="grid gap-4 sm:grid-cols-2">
            <FormField label="Contact group">
              <AppSelect v-model="form.groupId" placeholder="Optional group">
                <option value="">None — use pasted / CSV numbers</option>
                <option v-for="g in groups" :key="g.id" :value="g.id">
                  {{ g.name }} ({{ g.contactCount }})
                </option>
              </AppSelect>
            </FormField>
            <FormField label="Upload CSV" hint="One phone number per row or comma-separated">
              <input
                type="file"
                accept=".csv,text/csv"
                class="block w-full text-sm text-slate-600 file:mr-3 file:rounded-lg file:border-0 file:bg-brand-50 file:px-3 file:py-2 file:text-sm file:font-medium file:text-brand-700"
                @change="onCsvUpload"
              />
              <p v-if="csvHint" class="mt-1 text-xs text-brand-700">{{ csvHint }}</p>
            </FormField>
          </div>

          <FormField label="Paste phone numbers" hint="One per line, or comma-separated">
            <AppInput
              v-model="form.pasteNumbers"
              type="textarea"
              :rows="5"
              placeholder="0712345678&#10;0722334455"
              :disabled="Boolean(form.groupId)"
            />
          </FormField>

          <FormField label="Message" required>
            <AppInput v-model="form.message" type="textarea" :rows="5" />
          </FormField>

          <p v-if="success" class="text-sm text-emerald-700">{{ success }}</p>
          <p v-if="error" class="text-sm text-rose-600">{{ error }}</p>

          <AppButton
            type="submit"
            :loading="sms.loading"
            :disabled="!form.message || recipientCount === 0"
          >
            Send campaign
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
            <dt class="text-slate-500">SMS pages</dt>
            <dd class="font-semibold text-slate-900">{{ pages }}</dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-slate-500">Unit cost</dt>
            <dd class="font-semibold text-slate-900">{{ formatCurrency(wallet.smsCost) }}</dd>
          </div>
          <div class="border-t border-slate-100 pt-4 flex justify-between">
            <dt class="text-slate-500">Estimated total</dt>
            <dd class="text-lg font-semibold text-brand-700">{{ formatCurrency(cost) }}</dd>
          </div>
        </dl>
      </AppCard>
    </div>
  </div>
</template>
