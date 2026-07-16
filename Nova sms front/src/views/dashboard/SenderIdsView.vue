<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useSmsStore } from '@/stores/sms.store'
import { useAuthStore } from '@/stores/auth.store'
import { senderIdService } from '@/api/senderid.service'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import FormField from '@/components/common/FormField.vue'
import DataTable from '@/components/tables/DataTable.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import { formatDate } from '@/utils/format'

const sms = useSmsStore()
const auth = useAuthStore()
const senderName = ref('')
const message = ref('')
const error = ref('')

const pending = computed(() => sms.senderIds.filter((s) => s.status === 'PENDING'))

onMounted(() => sms.fetchSenderIds())

async function requestSender() {
  message.value = ''
  error.value = ''
  try {
    await sms.requestSenderId(senderName.value.trim())
    message.value = 'Sender ID submitted for approval.'
    senderName.value = ''
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Request failed'
  }
}

async function review(id: string, status: 'APPROVED' | 'REJECTED') {
  await senderIdService.review(id, {
    status,
    reason: status === 'REJECTED' ? 'Does not meet naming guidelines' : undefined,
  })
  await sms.fetchSenderIds()
}
</script>

<template>
  <div>
    <PageHeader
      title="Sender IDs"
      description="Request branded sender names and track approval status."
    />

    <div class="grid gap-6 lg:grid-cols-3">
      <AppCard
        v-if="!auth.isSuperAdmin"
        class="lg:col-span-1"
        title="Request sender ID"
        subtitle="3–11 characters, alphanumeric"
      >
        <form class="space-y-4" @submit.prevent="requestSender">
          <FormField label="Sender name" required>
            <AppInput v-model="senderName" placeholder="ACME" maxlength="11" />
          </FormField>
          <p v-if="message" class="text-sm text-emerald-700">{{ message }}</p>
          <p v-if="error" class="text-sm text-rose-600">{{ error }}</p>
          <AppButton type="submit" :loading="sms.loading" :disabled="senderName.length < 3">
            Submit request
          </AppButton>
        </form>
      </AppCard>

      <AppCard
        class="lg:col-span-2"
        :class="{ 'lg:col-span-3': auth.isSuperAdmin }"
        title="Your sender IDs"
        subtitle="Approved IDs can be used for outbound traffic"
        :padding="false"
      >
        <DataTable
          :columns="[
            { key: 'name', label: 'Sender name' },
            { key: 'status', label: 'Status' },
            { key: 'shared', label: 'Visibility' },
            { key: 'reason', label: 'Notes' },
            { key: 'date', label: 'Updated' },
            ...(auth.isSuperAdmin ? [{ key: 'actions', label: 'Actions' }] : []),
          ]"
        >
          <tr v-for="row in sms.senderIds" :key="row.id" class="hover:bg-slate-50/70">
            <td class="px-4 py-3 font-semibold text-slate-900">{{ row.senderName }}</td>
            <td class="px-4 py-3"><EntityStatusBadge :status="row.status" /></td>
            <td class="px-4 py-3 text-sm text-slate-600">
              {{ row.platformDefault ? 'Shared platform' : 'Organization' }}
            </td>
            <td class="max-w-xs truncate px-4 py-3 text-sm text-slate-500">
              {{ row.reason || '—' }}
            </td>
            <td class="px-4 py-3 text-sm text-slate-500">{{ formatDate(row.updatedAt) }}</td>
            <td v-if="auth.isSuperAdmin" class="px-4 py-3">
              <div v-if="row.status === 'PENDING'" class="flex gap-2">
                <AppButton size="sm" @click="review(row.id, 'APPROVED')">Approve</AppButton>
                <AppButton size="sm" variant="danger" @click="review(row.id, 'REJECTED')">
                  Reject
                </AppButton>
              </div>
            </td>
          </tr>
        </DataTable>
      </AppCard>
    </div>

    <p v-if="auth.isSuperAdmin && pending.length" class="mt-4 text-sm text-amber-700">
      {{ pending.length }} sender ID request(s) awaiting review.
    </p>
  </div>
</template>
