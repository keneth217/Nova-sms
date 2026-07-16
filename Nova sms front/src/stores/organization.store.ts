import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  AdminOrganization,
  OrganizationStatus,
  PlatformOverview,
} from '@/models/organization.model'
import { organizationService } from '@/api/organization.service'

export const useOrganizationStore = defineStore('organization', () => {
  const organizations = ref<AdminOrganization[]>([])
  const overview = ref<PlatformOverview | null>(null)
  const organizationName = ref('')
  const loading = ref(false)
  const error = ref<string | null>(null)
  const totalElements = ref(0)

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

  async function updateStatus(id: string, status: OrganizationStatus) {
    const updated = await organizationService.updateOrganizationStatus(id, status)
    const idx = organizations.value.findIndex((o) => o.id === id)
    if (idx >= 0) organizations.value[idx] = updated
    return updated
  }

  function setOrganizationName(name: string) {
    organizationName.value = name
  }

  return {
    organizations,
    overview,
    organizationName,
    loading,
    error,
    totalElements,
    fetchOverview,
    fetchOrganizations,
    updateStatus,
    setOrganizationName,
  }
})
