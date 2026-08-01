import api from './axios'
import type {
  ApiResponse,
  AuthResponse,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  LoginRequest,
  OrganizationRegisterRequest,
  ResetPasswordRequest,
} from '@/models/auth.model'
import type { Organization } from '@/models/organization.model'
import type { User } from '@/models/user.model'
import { normalizeLoginIdentifier, normalizePhone } from '@/utils/format'

class AuthService {
  async login(payload: LoginRequest): Promise<AuthResponse> {
    const identifier = normalizeLoginIdentifier(payload.email)
    const { data } = await api.post<ApiResponse<AuthResponse>>('/auth/login', {
      email: identifier,
      password: payload.password,
    })
    if (!data.success || !data.data) {
      throw new Error(data.message || 'Login failed')
    }
    return data.data
  }

  async register(payload: OrganizationRegisterRequest): Promise<Organization> {
    const body: OrganizationRegisterRequest = {
      ...payload,
      phone: normalizePhone(payload.phone),
      accountType: payload.accountType || 'BUSINESS',
    }
    const { data } = await api.post<ApiResponse<Organization>>('/organizations/register', body)
    if (!data.success || !data.data) {
      throw new Error(data.message || 'Registration failed')
    }
    return data.data
  }

  async getProfile(): Promise<User> {
    const { data } = await api.get<ApiResponse<User>>('/auth/me')
    if (!data.success || !data.data) throw new Error(data.message || 'Failed to load profile')
    return data.data
  }

  async changePassword(payload: ChangePasswordRequest): Promise<string> {
    const { data } = await api.post<ApiResponse<null>>('/auth/change-password', payload)
    if (!data.success) throw new Error(data.message || 'Failed to change password')
    return data.message
  }

  async forgotPassword(payload: ForgotPasswordRequest): Promise<string> {
    const { data } = await api.post<ApiResponse<null>>('/auth/forgot-password', payload)
    if (!data.success) throw new Error(data.message || 'Failed to request password reset')
    return data.message
  }

  async resetPassword(payload: ResetPasswordRequest): Promise<string> {
    const { data } = await api.post<ApiResponse<null>>('/auth/reset-password', payload)
    if (!data.success) throw new Error(data.message || 'Failed to reset password')
    return data.message
  }

  async refreshToken(_token: string): Promise<AuthResponse | null> {
    return null
  }
}

export const authService = new AuthService()
