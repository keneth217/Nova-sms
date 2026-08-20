import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  AdminOrganization,
  Organization,
  OrganizationStatus,
  PlatformOverview,
} from '@/models/organization.model'
import { organizationService } from '@/api/organization.service'

const ORG_NAME_KEY = 'nova_sms_org_name'

function loadStoredOrgName(): string {
  try {
    return localStorage.getItem(ORG_NAME_KEY) || ''
  } catch {
    return ''
  }
}

export const useOrganizationStore = defineStore('organization', () => {
  const organizations = ref<AdminOrganization[]>([])
  const overview = ref<PlatformOverview | null>(null)
  const currentOrganization = ref<Organization | null>(null)
  const organizationName = ref(loadStoredOrgName())
  const loading = ref(false)
  const error = ref<string | null>(null)
  const totalElements = ref(0)

  function setOrganizationName(name: string) {
    organizationName.value = name
    if (name) localStorage.setItem(ORG_NAME_KEY, name)
    else localStorage.removeItem(ORG_NAME_KEY)
  }

  async function fetchCurrentOrganization() {
    loading.value = true
    error.value = null
    try {
      currentOrganization.value = await organizationService.getCurrent()
      setOrganizationName(currentOrganization.value.name)
      return currentOrganization.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load organization'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchOverview() {
    loading.value = true
    error.value = null
    try {
      overview.value = await organizationService.getOverview()
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load overview'
    } finally {
      loading.value = false
    }
  }

  async function fetchOrganizations(
    params: {
      status?: OrganizationStatus
      search?: string
      page?: number
      size?: number
    } = {},
  ) {
    loading.value = true
    error.value = null
    try {
      const page = await organizationService.listOrganizations(params)
      organizations.value = page.content
      totalElements.value = page.totalElements
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load organizations'
    } finally {
      loading.value = false
    }
  }

  async function updateSettings(payload: {
    name?: string
    email?: string
    phone?: string
    notificationsEnabled: boolean
    lowBalanceThreshold: number
  }) {
    loading.value = true
    error.value = null
    try {
      currentOrganization.value = await organizationService.updateSettings(payload)
      setOrganizationName(currentOrganization.value.name)
      return currentOrganization.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to save settings'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updateStatus(id: string, status: OrganizationStatus) {
    const updated = await organizationService.updateOrganizationStatus(id, status)
    const idx = organizations.value.findIndex((o) => o.id === id)
    if (idx >= 0) organizations.value[idx] = updated
    return updated
  }

  function clearCurrentOrganization() {
    currentOrganization.value = null
    setOrganizationName('')
  }

  return {
    organizations,
    overview,
    currentOrganization,
    organizationName,
    loading,
    error,
    totalElements,
    fetchCurrentOrganization,
    fetchOverview,
    fetchOrganizations,
    updateStatus,
    updateSettings,
    setOrganizationName,
    clearCurrentOrganization,
  }
})
