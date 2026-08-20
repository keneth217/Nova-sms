<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { announcementService } from '@/api/announcement.service'
import type { Announcement, AnnouncementTone } from '@/models/announcement.model'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import FormField from '@/components/common/FormField.vue'
import AnnouncementBanner from '@/components/dashboard/AnnouncementBanner.vue'

const loading = ref(true)
const saving = ref(false)
const error = ref('')
const message = ref('')

const form = reactive({
  enabled: false,
  label: 'Announcement',
  title: 'Service Notice',
  body: '',
  tone: 'INFO' as AnnouncementTone,
  updatedAt: null as string | null,
})

function apply(data: Announcement) {
  form.enabled = data.enabled
  form.label = data.label || 'Announcement'
  form.title = data.title || 'Service Notice'
  form.body = data.body || ''
  form.tone = data.tone || 'INFO'
  form.updatedAt = data.updatedAt || null
}

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    apply(await announcementService.getAdmin())
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load announcement'
  } finally {
    loading.value = false
  }
})

async function save() {
  message.value = ''
  error.value = ''
  if (!form.label.trim() || !form.title.trim() || !form.body.trim()) {
    error.value = 'Label, title, and message are required.'
    return
  }
  saving.value = true
  try {
    apply(
      await announcementService.update({
        enabled: form.enabled,
        label: form.label.trim(),
        title: form.title.trim(),
        body: form.body.trim(),
        tone: form.tone,
      }),
    )
    message.value = form.enabled
      ? 'Announcement saved. Signed-in users will see it on the dashboard.'
      : 'Announcement saved and hidden from the dashboard.'
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to save announcement'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <PageHeader
      title="Announcement"
      description="Show a dismissible service notice on organization dashboards. Turn it on when you need to warn customers."
    />

    <p v-if="error" class="mb-4 text-sm text-rose-600">{{ error }}</p>
    <p v-else-if="loading" class="mb-4 text-sm text-slate-500">Loading announcement…</p>

    <form v-else class="space-y-6" @submit.prevent="save">
      <AppCard title="Banner" subtitle="Organizations see this on every dashboard page until they dismiss it. Updating the message shows it again.">
        <div class="space-y-4">
          <label class="flex items-start gap-3 text-sm">
            <input v-model="form.enabled" class="mt-1" type="checkbox" />
            <span>
              <span class="font-medium text-slate-800">Show announcement</span>
              <span class="mt-0.5 block text-xs text-slate-500">
                Off hides the banner for everyone, including Super Admin.
              </span>
            </span>
          </label>
          <div class="grid gap-4 sm:grid-cols-2">
            <FormField label="Header label" hint='Shown in the colored bar, for example "Announcement".' required>
              <AppInput v-model="form.label" maxlength="40" />
            </FormField>
            <FormField label="Title" hint='Shown above the message, for example "Service Notice".' required>
              <AppInput v-model="form.title" maxlength="120" />
            </FormField>
          </div>
          <FormField label="Tone">
            <select
              v-model="form.tone"
              class="w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20"
            >
              <option value="INFO">Info (purple)</option>
              <option value="WARNING">Warning (amber)</option>
              <option value="DANGER">Critical (red)</option>
            </select>
          </FormField>
          <FormField label="Message" required>
            <AppInput v-model="form.body" type="textarea" :rows="6" maxlength="2000" />
          </FormField>
        </div>
      </AppCard>

      <AppCard title="Preview" subtitle="This is how organizations will see the banner.">
        <AnnouncementBanner
          :announcement="{
            enabled: true,
            label: form.label || 'Announcement',
            title: form.title || 'Service Notice',
            body: form.body || 'Your message will appear here.',
            tone: form.tone,
          }"
          :dismissible="false"
        />
      </AppCard>

      <div class="flex flex-wrap items-center gap-3">
        <AppButton type="submit" :loading="saving">Save announcement</AppButton>
        <p v-if="message" class="text-sm text-brand-700">{{ message }}</p>
      </div>
    </form>
  </div>
</template>
