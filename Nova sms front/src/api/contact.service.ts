import api from './axios'
import type { ApiResponse, Page, PageRequest } from '@/models/auth.model'
import type {
  AddContactsToGroupRequest,
  AddToGroupResult,
  BulkContactImportRequest,
  Contact,
  ContactGroup,
  ContactGroupRequest,
  ContactRequest,
  ExcelImportResult,
  ImportResult,
} from '@/models/contact.model'
import { delay, isMockMode, normalizePhone } from '@/utils/format'
import { mockContacts, mockGroups, toPage } from '@/mocks/data'

class ContactService {
  async listGroups(): Promise<ContactGroup[]> {
    if (isMockMode()) {
      await delay(250)
      return [...mockGroups]
    }
    const { data } = await api.get<ApiResponse<ContactGroup[]>>('/contacts/groups')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load groups')
    return data.data
  }

  async createGroup(payload: ContactGroupRequest): Promise<ContactGroup> {
    if (isMockMode()) {
      await delay(350)
      const group: ContactGroup = {
        id: 'grp-' + Date.now(),
        name: payload.name,
        description: payload.description ?? null,
        contactCount: 0,
        createdAt: new Date().toISOString(),
      }
      mockGroups.unshift(group)
      return group
    }
    const { data } = await api.post<ApiResponse<ContactGroup>>('/contacts/groups', payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to create group')
    return data.data
  }

  async listContacts(params: PageRequest & { groupId?: string } = {}): Promise<Page<Contact>> {
    if (isMockMode()) {
      await delay(300)
      let items = [...mockContacts]
      if (params.groupId) {
        items = items.filter((c) => c.groupIds.includes(params.groupId!))
      }
      return toPage(items, params.page ?? 0, params.size ?? 50)
    }
    const { data } = await api.get<ApiResponse<Page<Contact>>>('/contacts', { params })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load contacts')
    return data.data
  }

  async createContact(payload: ContactRequest): Promise<Contact> {
    const phone = normalizePhone(payload.phone)
    if (isMockMode()) {
      await delay(350)
      const contact: Contact = {
        id: 'c-' + Date.now(),
        phone,
        firstName: payload.firstName ?? null,
        lastName: payload.lastName ?? null,
        email: payload.email ?? null,
        groupIds: payload.groupId ? [payload.groupId] : [],
        groupNames: [],
        createdAt: new Date().toISOString(),
      }
      mockContacts.unshift(contact)
      return contact
    }
    const { data } = await api.post<ApiResponse<Contact>>('/contacts', { ...payload, phone })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to create contact')
    return data.data
  }

  async importContacts(payload: BulkContactImportRequest): Promise<ImportResult> {
    const contacts = payload.contacts.map((c) => ({ ...c, phone: normalizePhone(c.phone) }))
    if (isMockMode()) {
      await delay(500)
      return { created: contacts.length, skipped: 0 }
    }
    const { data } = await api.post<ApiResponse<ImportResult>>('/contacts/import', {
      ...payload,
      contacts,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Import failed')
    return data.data
  }

  async importExcel(file: File, groupId?: string): Promise<ExcelImportResult> {
    if (isMockMode()) {
      await delay(700)
      return { created: 24, skipped: 2, invalid: 1, errors: ['Row 8: invalid phone'] }
    }
    const form = new FormData()
    form.append('file', file)
    const { data } = await api.post<ApiResponse<ExcelImportResult>>(
      '/contacts/import/excel',
      form,
      {
        params: { groupId },
        headers: { 'Content-Type': 'multipart/form-data' },
      },
    )
    if (!data.success || !data.data) throw new Error(data.message || 'Excel import failed')
    return data.data
  }

  async downloadExcelTemplate(): Promise<void> {
    const { downloadContactsExcelTemplate, downloadBlob } = await import('@/utils/excelTemplate')

    if (isMockMode()) {
      downloadContactsExcelTemplate()
      return
    }

    try {
      const { data } = await api.get<Blob>('/contacts/import/excel/template', {
        responseType: 'blob',
      })
      downloadBlob(data, 'nova-sms-contacts-template.xlsx')
    } catch {
      downloadContactsExcelTemplate()
    }
  }

  async addToGroup(groupId: string, payload: AddContactsToGroupRequest): Promise<AddToGroupResult> {
    if (isMockMode()) {
      await delay(300)
      return { groupId, added: payload.contactIds.length, totalInGroup: 10 }
    }
    const { data } = await api.post<ApiResponse<AddToGroupResult>>(
      `/contacts/groups/${groupId}/members`,
      payload,
    )
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to add contacts')
    return data.data
  }
}

export const contactService = new ContactService()
