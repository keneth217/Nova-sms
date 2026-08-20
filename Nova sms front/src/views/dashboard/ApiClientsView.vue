<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { apiClientService } from '@/api/api-client.service'
import { organizationService } from '@/api/organization.service'
import type { ApiClient, ApiPermission } from '@/models/api-client.model'
import { API_PERMISSIONS } from '@/models/api-client.model'
import type { AdminOrganization } from '@/models/organization.model'
import { useAuthStore } from '@/stores/auth.store'
import PageHeader from '@/components/common/PageHeader.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import FormField from '@/components/common/FormField.vue'
import AppModal from '@/components/common/AppModal.vue'
import DataTable from '@/components/tables/DataTable.vue'
import EntityStatusBadge from '@/components/common/EntityStatusBadge.vue'
import { formatDate } from '@/utils/format'

const DEFAULT_PERMISSIONS: ApiPermission[] = ['SMS_SEND', 'SMS_BULK', 'SMS_STATUS']

const auth = useAuthStore()
const isAdmin = computed(() => auth.isSuperAdmin)
const clients = ref<ApiClient[]>([])
const organizations = ref<AdminOrganization[]>([])
const loading = ref(false)
const message = ref('')
const revealedKey = ref('')
const showCreate = ref(false)
const editingClient = ref<ApiClient | null>(null)
const saving = ref(false)
const formError = ref('')
const form = reactive({
  organizationId: '',
  name: '',
  rateLimitPerMinute: 100,
  permissions: [...DEFAULT_PERMISSIONS] as ApiPermission[],
})

async function load() {
  loading.value = true
  try {
    if (isAdmin.value) {
      const [page, orgs] = await Promise.all([
        apiClientService.listAll({ size: 50 }),
        organizationService.listOrganizations({ size: 100 }),
      ])
      clients.value = page.content
      organizations.value = orgs.content
    } else {
      clients.value = await apiClientService.listMine()
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)

function togglePermission(permission: ApiPermission) {
  if (form.permissions.includes(permission)) {
    form.permissions = form.permissions.filter((item) => item !== permission)
  } else {
    form.permissions = [...form.permissions, permission]
  }
}

function openCreate() {
  formError.value = ''
  editingClient.value = null
  form.name = ''
  form.rateLimitPerMinute = 100
  form.permissions = [...DEFAULT_PERMISSIONS]
  form.organizationId = organizations.value[0]?.id || ''
  showCreate.value = true
}

function openEdit(client: ApiClient) {
  formError.value = ''
  editingClient.value = client
  form.name = client.name
  form.rateLimitPerMinute = client.rateLimitPerMinute
  form.permissions = [...client.permissions]
  form.organizationId = client.organizationId
  showCreate.value = true
}

async function saveClient() {
  formError.value = ''
  if (!form.name.trim()) {
    formError.value = 'Name is required.'
    return
  }
  if (!editingClient.value && isAdmin.value && !form.organizationId) {
    formError.value = 'Select an organization.'
    return
  }
  saving.value = true
  try {
    if (editingClient.value) {
      const payload = {
        name: form.name.trim(),
        rateLimitPerMinute: form.rateLimitPerMinute,
        permissions: form.permissions,
      }
      if (isAdmin.value) await apiClientService.updateAdmin(editingClient.value.id, payload)
      else await apiClientService.updateMine(editingClient.value.id, payload)
      message.value = `${form.name.trim()} updated. Wallet permissions apply on the next API request.`
      showCreate.value = false
      editingClient.value = null
      await load()
      return
    }
    const payload = {
      name: form.name.trim(),
      rateLimitPerMinute: form.rateLimitPerMinute,
      permissions: form.permissions,
      organizationId: isAdmin.value ? form.organizationId : undefined,
    }
    const created = isAdmin.value
      ? await apiClientService.createAdmin(payload)
      : await apiClientService.createMine(payload)
    revealedKey.value = created.apiKey
    showCreate.value = false
    message.value = 'API client created. Copy the key now — it will not be shown again.'
    await load()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : 'Failed to save API client'
  } finally {
    saving.value = false
  }
}

async function rotate(client: ApiClient) {
  if (!confirm(`Rotate the key for ${client.name}? The current key stops working immediately.`)) return
  try {
    const created = isAdmin.value
      ? await apiClientService.rotateAdmin(client.id)
      : await apiClientService.rotateMine(client.id)
    revealedKey.value = created.apiKey
    message.value = 'Key rotated. Copy the new key now.'
    await load()
  } catch (e) {
    message.value = e instanceof Error ? e.message : 'Failed to rotate key'
  }
}

async function setEnabled(client: ApiClient, enabled: boolean) {
  try {
    if (isAdmin.value) await apiClientService.setEnabledAdmin(client.id, enabled)
    else await apiClientService.setEnabledMine(client.id, enabled)
    await load()
  } catch (e) {
    message.value = e instanceof Error ? e.message : 'Failed to update client'
  }
}

async function revoke(client: ApiClient) {
  if (!confirm(`Revoke ${client.name}? This cannot be undone.`)) return
  try {
    if (isAdmin.value) await apiClientService.revokeAdmin(client.id)
    else await apiClientService.revokeMine(client.id)
    message.value = `${client.name} revoked.`
    await load()
  } catch (e) {
    message.value = e instanceof Error ? e.message : 'Failed to revoke client'
  }
}
</script>

<template>
  <div>
    <PageHeader
      title="API clients"
      description="Issue Nova SMS keys for your own apps. Grant WALLET_READ and WALLET_TOPUP so those apps can show balance and accept M-Pesa top-ups on their own site."
    >
      <template #actions>
        <AppButton @click="openCreate">New API client</AppButton>
      </template>
    </PageHeader>

    <p v-if="message" class="mb-4 text-sm text-emerald-700">{{ message }}</p>
    <AppCard v-if="revealedKey" class="mb-6" title="Copy this API key now">
      <p class="text-sm text-slate-600">Nova will not display the full key again.</p>
      <code class="mt-3 block break-all rounded-lg bg-slate-900 px-3 py-2 text-sm text-emerald-300">{{
        revealedKey
      }}</code>
      <AppButton class="mt-3" size="sm" variant="secondary" @click="revealedKey = ''">Dismiss</AppButton>
    </AppCard>

    <p v-if="loading" class="text-sm text-slate-500">Loading API clients…</p>
    <DataTable
      v-else
      :columns="[
        { key: 'name', label: 'Application' },
        { key: 'org', label: 'Organization' },
        { key: 'status', label: 'Status' },
        { key: 'permissions', label: 'Permissions' },
        { key: 'rate', label: 'Rate limit' },
        { key: 'created', label: 'Created' },
        { key: 'used', label: 'Last used' },
        { key: 'actions', label: '' },
      ]"
      empty-title="No API clients yet"
    >
      <tr v-for="client in clients" :key="client.id">
        <td class="px-4 py-3">
          <p class="font-medium text-slate-900">{{ client.name }}</p>
          <p class="font-mono text-xs text-slate-500">{{ client.apiKeyPrefix }}…</p>
        </td>
        <td class="px-4 py-3 text-sm">{{ client.organizationName || '—' }}</td>
        <td class="px-4 py-3"><EntityStatusBadge :status="client.status" /></td>
        <td class="px-4 py-3 text-xs text-slate-600">{{ client.permissions.join(', ') }}</td>
        <td class="px-4 py-3 text-sm">{{ client.rateLimitPerMinute }}/min</td>
        <td class="px-4 py-3 text-sm text-slate-500">{{ formatDate(client.createdAt, false) }}</td>
        <td class="px-4 py-3 text-sm text-slate-500">{{ formatDate(client.lastUsedAt, false) || '—' }}</td>
        <td class="px-4 py-3">
          <div class="flex flex-wrap justify-end gap-2">
            <AppButton
              v-if="client.status === 'ACTIVE'"
              size="sm"
              variant="ghost"
              @click="setEnabled(client, false)"
            >
              Disable
            </AppButton>
            <AppButton
              v-if="client.status === 'DISABLED'"
              size="sm"
              variant="ghost"
              @click="setEnabled(client, true)"
            >
              Enable
            </AppButton>
            <AppButton
              v-if="client.status !== 'REVOKED'"
              size="sm"
              variant="ghost"
              @click="openEdit(client)"
            >
              Permissions
            </AppButton>
            <AppButton
              v-if="client.status !== 'REVOKED'"
              size="sm"
              variant="ghost"
              @click="rotate(client)"
            >
              Rotate
            </AppButton>
            <AppButton
              v-if="client.status !== 'REVOKED'"
              size="sm"
              variant="ghost"
              @click="revoke(client)"
            >
              Revoke
            </AppButton>
          </div>
        </td>
      </tr>
    </DataTable>

    <AppModal
      :open="showCreate"
      :title="editingClient ? `Edit ${editingClient.name}` : 'Create API client'"
      @close="showCreate = false"
    >
      <form id="create-api-client" class="space-y-4" @submit.prevent="saveClient">
        <FormField v-if="isAdmin && !editingClient" label="Organization" required>
          <AppSelect v-model="form.organizationId">
            <option value="" disabled>Select organization</option>
            <option v-for="org in organizations" :key="org.id" :value="org.id">{{ org.name }}</option>
          </AppSelect>
        </FormField>
        <FormField label="Name" required>
          <AppInput v-model="form.name" placeholder="e.g. Mwalimu Backend" />
        </FormField>
        <FormField label="Rate limit per minute">
          <AppInput v-model.number="form.rateLimitPerMinute" type="number" min="1" max="10000" />
        </FormField>
        <FormField
          label="Permissions"
          hint="WALLET_READ and WALLET_TOPUP let the integrating app show balance and top up on its own site instead of the Nova SMS portal."
        >
          <div class="flex flex-col gap-2">
            <label
              v-for="permission in API_PERMISSIONS"
              :key="permission.id"
              class="flex items-start gap-2 text-sm"
            >
              <input
                class="mt-0.5"
                type="checkbox"
                :checked="form.permissions.includes(permission.id)"
                @change="togglePermission(permission.id)"
              />
              <span>
                <span class="font-medium text-slate-800">{{ permission.label }}</span>
                <span class="block text-xs text-slate-500">{{ permission.hint }}</span>
              </span>
            </label>
          </div>
        </FormField>
        <p v-if="formError" class="text-sm text-rose-600">{{ formError }}</p>
      </form>
      <template #footer>
        <AppButton variant="secondary" :disabled="saving" @click="showCreate = false">Cancel</AppButton>
        <AppButton type="submit" form="create-api-client" :loading="saving">
          {{ editingClient ? 'Save permissions' : 'Create' }}
        </AppButton>
      </template>
    </AppModal>
  </div>
</template>
