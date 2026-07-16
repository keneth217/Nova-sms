export interface ContactGroup {
  id: string
  name: string
  description: string | null
  contactCount: number
  createdAt: string
}

export interface ContactGroupRequest {
  name: string
  description?: string
}

export interface Contact {
  id: string
  phone: string
  firstName: string | null
  lastName: string | null
  email: string | null
  groupIds: string[]
  groupNames: string[]
  createdAt: string
}

export interface ContactRequest {
  phone: string
  firstName?: string
  lastName?: string
  email?: string
  groupId?: string
}

export interface BulkContactImportRequest {
  contacts: ContactRequest[]
  groupId?: string
}

export interface AddContactsToGroupRequest {
  contactIds: string[]
}

export interface AddToGroupResult {
  groupId: string
  added: number
  totalInGroup: number
}

export interface ImportResult {
  created: number
  skipped: number
}

export interface ExcelImportResult extends ImportResult {
  invalid: number
  errors: string[]
}
