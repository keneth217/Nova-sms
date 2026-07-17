import type { OrganizationAccountType } from './organization.model'

export type UserRole = 'SUPER_ADMIN' | 'ORGANIZATION_ADMIN'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T | null
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
  numberOfElements: number
}

export interface PageRequest {
  page?: number
  size?: number
  sort?: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  userId: string
  email: string
  fullName: string
  role: UserRole
  organizationId: string | null
  organizationName?: string | null
  accountType?: OrganizationAccountType | null
  expiresAt?: string | null
}

export interface OrganizationRegisterRequest {
  name: string
  email: string
  phone: string
  password: string
  adminFullName: string
  accountType: OrganizationAccountType
  termsAccepted: boolean
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface ForgotPasswordRequest {
  email: string
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
}

export interface AuthUser {
  userId: string
  email: string
  fullName: string
  role: UserRole
  organizationId: string | null
  organizationName?: string | null
  accountType?: OrganizationAccountType | null
  expiresAt?: string | null
}
