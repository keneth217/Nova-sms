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
import { normalizePhone } from '@/utils/format'

class ContactService {
  async listGroups(): Promise<ContactGroup[]> {
    const { data } = await api.get<ApiResponse<ContactGroup[]>>('/contacts/groups')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load groups')
    return data.data
  }

  async createGroup(payload: ContactGroupRequest): Promise<ContactGroup> {
    const { data } = await api.post<ApiResponse<ContactGroup>>('/contacts/groups', payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to create group')
    return data.data
  }

  async updateGroup(groupId: string, payload: ContactGroupRequest): Promise<ContactGroup> {
    const { data } = await api.patch<ApiResponse<ContactGroup>>(`/contacts/groups/${groupId}`, payload)
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to update group')
    return data.data
  }

  async deleteGroup(groupId: string): Promise<void> {
    await api.delete(`/contacts/groups/${groupId}`)
  }

  async listContacts(params: PageRequest & { groupId?: string } = {}): Promise<Page<Contact>> {
    const { data } = await api.get<ApiResponse<Page<Contact>>>('/contacts', { params })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load contacts')
    return data.data
  }

  async createContact(payload: ContactRequest): Promise<Contact> {
    const phone = normalizePhone(payload.phone)
    const { data } = await api.post<ApiResponse<Contact>>('/contacts', { ...payload, phone })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to create contact')
    return data.data
  }

  async updateContact(contactId: string, payload: ContactRequest): Promise<Contact> {
    const phone = normalizePhone(payload.phone)
    const { data } = await api.patch<ApiResponse<Contact>>(`/contacts/${contactId}`, { ...payload, phone })
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to update contact')
    return data.data
  }

  async deleteContact(contactId: string): Promise<void> {
    await api.delete(`/contacts/${contactId}`)
  }

  async importContacts(payload: BulkContactImportRequest): Promise<ImportResult> {
    const contacts = payload.contacts.map((c) => ({ ...c, phone: normalizePhone(c.phone) }))
    const { data } = await api.post<ApiResponse<ImportResult>>('/contacts/import', {
      ...payload,
      contacts,
    })
    if (!data.success || !data.data) throw new Error(data.message || 'Import failed')
    return data.data
  }

  async importExcel(file: File, groupId?: string): Promise<ExcelImportResult> {
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
    const { data } = await api.post<ApiResponse<AddToGroupResult>>(
      `/contacts/groups/${groupId}/members`,
      payload,
    )
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to add contacts')
    return data.data
  }

  async removeFromGroup(groupId: string, contactId: string): Promise<void> {
    await api.delete(`/contacts/groups/${groupId}/members/${contactId}`)
  }
}

export const contactService = new ContactService()
