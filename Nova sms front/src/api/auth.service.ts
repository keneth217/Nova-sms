import api from './axios'
import type {
  ApiResponse,
  AuthResponse,
  LoginRequest,
  OrganizationRegisterRequest,
} from '@/models/auth.model'
import type { Organization } from '@/models/organization.model'
import {
  delay,
  isMockMode,
  looksLikePhone,
  normalizeLoginIdentifier,
  normalizePhone,
} from '@/utils/format'
import { mockEventAdmin, mockOrgAdmin, mockSuperAdmin } from '@/mocks/data'

class AuthService {
  async login(payload: LoginRequest): Promise<AuthResponse> {
    const identifier = normalizeLoginIdentifier(payload.email)

    if (isMockMode()) {
      await delay(400)
      if (identifier === 'admin@novastack.com' && payload.password === 'ChangeMe123!') {
        return mockSuperAdmin
      }
      if (
        (identifier === 'admin@acme.co.ke' ||
          identifier === '254712345678' ||
          normalizePhone(payload.email) === '254712345678') &&
        payload.password === 'password123'
      ) {
        return mockOrgAdmin
      }
      if (
        (identifier === 'family@example.com' || identifier === '254711223344') &&
        payload.password === 'password123'
      ) {
        return mockEventAdmin
      }
      if (payload.password.length >= 6) {
        return {
          ...mockOrgAdmin,
          email: looksLikePhone(payload.email) ? mockOrgAdmin.email : identifier,
          fullName: 'Demo User',
        }
      }
      throw new Error('Invalid email/phone or password')
    }

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

    if (isMockMode()) {
      await delay(500)
      const isEvent = body.accountType === 'EVENT'
      return {
        id: 'org-new',
        name: body.name,
        email: body.email,
        phone: body.phone,
        apiKey: 'nsk_demo_' + Math.random().toString(36).slice(2, 10),
        mpesaAccountRef: body.name.slice(0, 8).toUpperCase().replace(/\s/g, '') + '-001',
        status: 'ACTIVE',
        accountType: body.accountType,
        expiresAt: isEvent
          ? new Date(Date.now() + 7 * 86_400_000).toISOString()
          : null,
        activeDays: isEvent ? 7 : null,
        createdAt: new Date().toISOString(),
        walletId: 'wal-new',
        walletBalance: 0,
        walletCurrency: 'KES',
      }
    }

    const { data } = await api.post<ApiResponse<Organization>>('/organizations/register', body)
    if (!data.success || !data.data) {
      throw new Error(data.message || 'Registration failed')
    }
    return data.data
  }

  async refreshToken(_token: string): Promise<AuthResponse | null> {
    return null
  }
}

export const authService = new AuthService()
