<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ArrowDownTrayIcon,
  DocumentArrowDownIcon,
  DocumentArrowUpIcon,
  DocumentTextIcon,
} from '@heroicons/vue/24/outline'
import { contactService } from '@/api/contact.service'
import type { Contact, ContactGroup } from '@/models/contact.model'
import { useOrganizationStore } from '@/stores/organization.store'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import FormField from '@/components/common/FormField.vue'
import AppModal from '@/components/common/AppModal.vue'
import DataTable from '@/components/tables/DataTable.vue'
import { formatDate, parsePhoneList } from '@/utils/format'
import { exportContactsToExcel } from '@/utils/exportExcel'
import { exportContactsToPdf } from '@/utils/exportPdf'

const orgStore = useOrganizationStore()
const groups = ref<ContactGroup[]>([])
const contacts = ref<Contact[]>([])
const selectedGroup = ref('')
const search = ref('')
const loading = ref(false)
const importing = ref(false)
const downloadingTemplate = ref(false)
const exporting = ref(false)
const message = ref('')
const importError = ref('')

const showGroupModal = ref(false)
const showContactModal = ref(false)
const showImportModal = ref(false)

const groupForm = reactive({ name: '', description: '' })
const contactForm = reactive({
  phone: '',
  firstName: '',
  lastName: '',
  email: '',
  groupId: '',
})
const importText = ref('')
const importGroupId = ref('')
const excelFile = ref<File | null>(null)

const filteredContacts = computed(() => {
  const q = search.value.toLowerCase()
  return contacts.value.filter((c) => {
    if (selectedGroup.value && !c.groupIds.includes(selectedGroup.value)) return false
    if (!q) return true
    return (
      c.phone.includes(q) ||
      (c.firstName || '').toLowerCase().includes(q) ||
      (c.lastName || '').toLowerCase().includes(q) ||
      (c.email || '').toLowerCase().includes(q)
    )
  })
})

const selectedGroupName = computed(
  () => groups.value.find((g) => g.id === selectedGroup.value)?.name,
)

const exportMeta = computed(() => ({
  organizationName: orgStore.organizationName || 'Organization',
  groupFilter: selectedGroupName.value,
}))

async function load() {
  loading.value = true
  try {
    groups.value = await contactService.listGroups()
    const page = await contactService.listContacts({
      groupId: selectedGroup.value || undefined,
      size: 50,
    })
    contacts.value = page.content
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function createGroup() {
  await contactService.createGroup({ ...groupForm })
  showGroupModal.value = false
  groupForm.name = ''
  groupForm.description = ''
  message.value = 'Group created.'
  await load()
}

async function createContact() {
  await contactService.createContact({
    phone: contactForm.phone,
    firstName: contactForm.firstName || undefined,
    lastName: contactForm.lastName || undefined,
    email: contactForm.email || undefined,
    groupId: contactForm.groupId || undefined,
  })
  showContactModal.value = false
  Object.assign(contactForm, { phone: '', firstName: '', lastName: '', email: '', groupId: '' })
  message.value = 'Contact added.'
  await load()
}

function onExcelSelected(event: Event) {
  const input = event.target as HTMLInputElement
  excelFile.value = input.files?.[0] ?? null
  importError.value = ''
}

async function downloadTemplate() {
  downloadingTemplate.value = true
  importError.value = ''
  try {
    await contactService.downloadExcelTemplate()
    message.value = 'Excel template downloaded.'
  } catch (e) {
    importError.value = e instanceof Error ? e.message : 'Failed to download template'
  } finally {
    downloadingTemplate.value = false
  }
}

async function exportExcelList() {
  if (!filteredContacts.value.length) {
    message.value = 'No contacts to export.'
    return
  }
  exporting.value = true
  try {
    exportContactsToExcel(filteredContacts.value, exportMeta.value)
    message.value = `Exported ${filteredContacts.value.length} contacts to Excel.`
  } catch (e) {
    message.value = e instanceof Error ? e.message : 'Excel export failed'
  } finally {
    exporting.value = false
  }
}

async function exportPdfList() {
  if (!filteredContacts.value.length) {
    message.value = 'No contacts to export.'
    return
  }
  exporting.value = true
  try {
    exportContactsToPdf(filteredContacts.value, exportMeta.value)
    message.value = `Exported ${filteredContacts.value.length} contacts to PDF.`
  } catch (e) {
    message.value = e instanceof Error ? e.message : 'PDF export failed'
  } finally {
    exporting.value = false
  }
}

async function importContacts() {
  importError.value = ''
  importing.value = true
  try {
    if (excelFile.value) {
      const result = await contactService.importExcel(
        excelFile.value,
        importGroupId.value || undefined,
      )
      message.value = `Excel import: ${result.created} created, ${result.skipped} skipped${
        result.invalid ? `, ${result.invalid} invalid` : ''
      }.`
      if (result.errors?.length) {
        importError.value = result.errors.slice(0, 3).join(' · ')
      }
    } else if (importText.value.trim()) {
      const phones = parsePhoneList(importText.value)
      await contactService.importContacts({
        contacts: phones.map((phone) => ({ phone })),
        groupId: importGroupId.value || undefined,
      })
      message.value = `Imported ${phones.length} contacts.`
    } else {
      importError.value = 'Upload an Excel file or paste phone numbers.'
      return
    }

    showImportModal.value = false
    importText.value = ''
    excelFile.value = null
    await load()
  } catch (e) {
    importError.value = e instanceof Error ? e.message : 'Import failed'
  } finally {
    importing.value = false
  }
}

function openImportModal() {
  importError.value = ''
  excelFile.value = null
  importText.value = ''
  showImportModal.value = true
}
</script>

<template>
  <div>
    <PageHeader
      title="Contacts"
      description="Organize recipients into groups and import lists for campaigns."
    >
      <template #actions>
        <AppButton
          variant="secondary"
          :loading="exporting"
          :disabled="!filteredContacts.length"
          @click="exportExcelList"
        >
          <DocumentArrowDownIcon class="h-4 w-4" />
          Excel
        </AppButton>
        <AppButton
          variant="secondary"
          :loading="exporting"
          :disabled="!filteredContacts.length"
          @click="exportPdfList"
        >
          <DocumentTextIcon class="h-4 w-4" />
          PDF
        </AppButton>
        <AppButton variant="secondary" @click="openImportModal">Import</AppButton>
        <AppButton variant="secondary" @click="showGroupModal = true">New group</AppButton>
        <AppButton @click="showContactModal = true">Add contact</AppButton>
      </template>
    </PageHeader>

    <p v-if="message" class="mb-4 text-sm text-emerald-700">{{ message }}</p>

    <div class="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      <button
        v-for="group in groups"
        :key="group.id"
        type="button"
        class="rounded-xl border p-4 text-left transition"
        :class="
          selectedGroup === group.id
            ? 'border-brand-500 bg-brand-50 shadow-sm'
            : 'border-slate-200 bg-white hover:border-slate-300'
        "
        @click="selectedGroup = selectedGroup === group.id ? '' : group.id"
      >
        <p class="text-sm font-semibold text-slate-900">{{ group.name }}</p>
        <p class="mt-1 text-xs text-slate-500">{{ group.description || 'No description' }}</p>
        <p class="mt-3 text-2xl font-semibold text-brand-700">{{ group.contactCount }}</p>
        <p class="text-xs text-slate-400">contacts</p>
      </button>
    </div>

    <AppCard title="Contact directory" :padding="false">
      <div class="flex flex-wrap gap-3 border-b border-slate-100 px-5 py-4">
        <div class="min-w-[220px] flex-1">
          <AppInput v-model="search" placeholder="Search contacts…" />
        </div>
        <AppSelect v-model="selectedGroup" class="w-48">
          <option value="">All groups</option>
          <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
        </AppSelect>
      </div>

      <DataTable
        :columns="[
          { key: 'name', label: 'Name' },
          { key: 'phone', label: 'Phone' },
          { key: 'email', label: 'Email' },
          { key: 'groups', label: 'Groups' },
          { key: 'created', label: 'Added' },
        ]"
        empty-title="No contacts yet"
      >
        <tr v-for="c in filteredContacts" :key="c.id" class="hover:bg-slate-50/70">
          <td class="px-4 py-3 font-medium text-slate-800">
            {{ [c.firstName, c.lastName].filter(Boolean).join(' ') || '—' }}
          </td>
          <td class="px-4 py-3 font-mono text-xs">{{ c.phone }}</td>
          <td class="px-4 py-3 text-slate-600">{{ c.email || '—' }}</td>
          <td class="px-4 py-3 text-slate-600">{{ c.groupNames.join(', ') || '—' }}</td>
          <td class="px-4 py-3 text-slate-500">{{ formatDate(c.createdAt, false) }}</td>
        </tr>
      </DataTable>
    </AppCard>

    <AppModal :open="showGroupModal" title="Create contact group" @close="showGroupModal = false">
      <form class="space-y-4" @submit.prevent="createGroup">
        <FormField label="Name" required>
          <AppInput v-model="groupForm.name" />
        </FormField>
        <FormField label="Description">
          <AppInput v-model="groupForm.description" type="textarea" :rows="3" />
        </FormField>
      </form>
      <template #footer>
        <AppButton variant="secondary" @click="showGroupModal = false">Cancel</AppButton>
        <AppButton :disabled="!groupForm.name" @click="createGroup">Create</AppButton>
      </template>
    </AppModal>

    <AppModal :open="showContactModal" title="Add contact" @close="showContactModal = false">
      <form class="space-y-4" @submit.prevent="createContact">
        <FormField label="Phone" required>
          <AppInput v-model="contactForm.phone" type="tel" placeholder="0712345678" />
        </FormField>
        <div class="grid grid-cols-2 gap-3">
          <FormField label="First name">
            <AppInput v-model="contactForm.firstName" />
          </FormField>
          <FormField label="Last name">
            <AppInput v-model="contactForm.lastName" />
          </FormField>
        </div>
        <FormField label="Email">
          <AppInput v-model="contactForm.email" type="email" />
        </FormField>
        <FormField label="Group">
          <AppSelect v-model="contactForm.groupId">
            <option value="">None</option>
            <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
          </AppSelect>
        </FormField>
      </form>
      <template #footer>
        <AppButton variant="secondary" @click="showContactModal = false">Cancel</AppButton>
        <AppButton :disabled="!contactForm.phone" @click="createContact">Save</AppButton>
      </template>
    </AppModal>

    <AppModal
      :open="showImportModal"
      title="Import contacts"
      subtitle="Download the sample Excel template, fill it in, then upload — or paste numbers."
      size="lg"
      @close="showImportModal = false"
    >
      <div class="space-y-5">
        <div class="rounded-xl border border-brand-100 bg-brand-50/80 p-4">
          <p class="text-sm font-semibold text-brand-900">Excel sample format</p>
          <p class="mt-1 text-xs leading-relaxed text-brand-800">
            Required column: <code class="font-mono">phone</code>. Optional:
            <code class="font-mono">firstName</code>,
            <code class="font-mono">lastName</code>,
            <code class="font-mono">email</code>. Phones can be
            <code class="font-mono">07…</code> or <code class="font-mono">254…</code>.
          </p>
          <AppButton
            class="mt-3"
            size="sm"
            variant="secondary"
            :loading="downloadingTemplate"
            @click="downloadTemplate"
          >
            <ArrowDownTrayIcon class="h-4 w-4" />
            Download Excel template
          </AppButton>
        </div>

        <FormField label="Target group">
          <AppSelect v-model="importGroupId">
            <option value="">No group</option>
            <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
          </AppSelect>
        </FormField>

        <FormField label="Upload Excel (.xlsx / .xls)">
          <label
            class="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-slate-300 bg-slate-50 px-4 py-6 text-center transition hover:border-brand-400 hover:bg-brand-50/40"
          >
            <DocumentArrowUpIcon class="h-8 w-8 text-slate-400" />
            <span class="text-sm font-medium text-slate-700">
              {{ excelFile ? excelFile.name : 'Choose Excel file' }}
            </span>
            <span class="text-xs text-slate-500">Matches the sample template columns</span>
            <input
              type="file"
              accept=".xlsx,.xls,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel"
              class="hidden"
              @change="onExcelSelected"
            />
          </label>
        </FormField>

        <div class="relative text-center text-xs font-medium uppercase tracking-wide text-slate-400">
          <span class="relative z-10 bg-white px-2">or paste phones</span>
          <span class="absolute inset-x-0 top-1/2 border-t border-slate-200" />
        </div>

        <FormField label="Phone numbers" hint="One per line, or comma-separated">
          <AppInput
            v-model="importText"
            type="textarea"
            :rows="5"
            placeholder="0712345678&#10;0722334455"
          />
        </FormField>

        <p v-if="importError" class="text-sm text-rose-600">{{ importError }}</p>
      </div>
      <template #footer>
        <AppButton variant="secondary" @click="showImportModal = false">Cancel</AppButton>
        <AppButton
          :loading="importing"
          :disabled="!excelFile && !importText.trim()"
          @click="importContacts"
        >
          Import
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
