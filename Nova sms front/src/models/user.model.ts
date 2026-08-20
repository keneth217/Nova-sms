import type { UserRole } from './auth.model'

export interface User {
  id: string
  email: string
  phone?: string | null
  fullName: string
  role: UserRole
  enabled: boolean
  organizationId: string | null
  organizationName: string | null
  createdAt: string
}
